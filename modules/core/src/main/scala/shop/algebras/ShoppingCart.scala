package shop.algebras

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import shop.config.Data.ShoppingCartExpiration
import shop.domain.Auth.UserId
import shop.domain.Item.ItemId
import shop.domain.ShoppingCart.{ Cart, CartItem, CartTotal, Quantity }
import shop.effects.CommonEffects.{ ApThrow, MonadThrow }
import shop.effects.GenUUID
import squants.Money
import squants.market.USD

trait ShoppingCart[F[_]] {
  def add(
      userId: UserId,
      itemId: ItemId,
      quantity: Quantity
  ): F[Unit]

  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object LiveShoppingCart {
  def make[F[_]: Sync: GenUUID: MonadThrow](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      exp: ShoppingCartExpiration
  ): F[LiveShoppingCart[F]] = Sync[F].delay(
    new LiveShoppingCart[F](items, redis, exp)
  )
}
final class LiveShoppingCart[F[_]: GenUUID: MonadThrow] private (
    items: Items[F],
    redis: RedisCommands[F, String, String],
    exp: ShoppingCartExpiration
) extends ShoppingCart[F] {

  override def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit] =
    redis.hSet(
      userId.value.toString,
      itemId.value.toString,
      quantity.value.toString
    ) *>
      redis.expire(
        userId.value.toString,
        exp.value
      )

  override def get(userId: UserId): F[CartTotal] = {
    val itemsResult: F[List[CartItem]] = redis.hGetAll(userId.value.toString).flatMap {
      it =>
        it.toList.traverseFilter { // get all of the items in a shopping cart
          case (k, v) =>
            for {
              itemId <- GenUUID[F].read[ItemId](k) // convert them into itemUuid and Quantity
              quant <- ApThrow[F].catchNonFatal(Quantity(v.toInt))
              res <- items
                       .findById(itemId) // fetch that item from the real psql item table
                       .map {
                         iList =>
                           iList.map(itm => CartItem(itm, quant)) // iff it exists, return it with the quantity
                       }
            } yield res
        }
    }

    itemsResult.map(iList => CartTotal(iList, calcTotal(iList)))
  }

  override def delete(userId: UserId): F[Unit] = redis.del(userId.value.toString)

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.value.toString, itemId.value.toString)

  override def update(userId: UserId, cart: Cart): F[Unit] =
    redis.hGetAll(userId.value.toString).flatMap {
      itemMap =>
        itemMap.toList.traverse_ {
          case (itemIdKey, _) =>
            GenUUID[F].read[ItemId](itemIdKey).flatMap {
              itemId =>
                cart.items.get(itemId).traverse_ {
                  quant =>
                    redis.hSet(userId.value.toString, itemIdKey, quant.value.toString)
                }
            }
        } *> redis.expire(userId.value.toString, exp.value)
    }

  private def calcTotal(items: List[CartItem]): Money = {
    val total: BigDecimal = items.foldMap {
      itm =>
        itm.item.price.amount * itm.quantity.value
    }
    USD(total)
  }
}
