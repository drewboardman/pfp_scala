package shop.algebras

import cats.effect.{ Resource, Sync }
import cats.implicits._
import shop.domain.Auth.UserId
import shop.domain.Item._
import shop.domain.Orders.{ Order, OrderId, PaymentId }
import shop.domain.ShoppingCart.{ CartItem, Quantity }
import shop.effects.GenUUID
import shop.ext.skunkx.CodecOps
import shop.http.Json._
import skunk._
import skunk.circe.codec.all._
import skunk.codec.all._
import skunk.implicits._
import squants.market.{ Money, USD }

trait Orders[F[_]] {
  def get(
      userId: UserId,
      orderId: OrderId
  ): F[Option[Order]]

  def findBy(userId: UserId): F[List[Order]]

  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId]
}

object LiveOrders {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[LiveOrders[F]] = Sync[F].delay(
    new LiveOrders[F](sessionPool)
  )
}

final class LiveOrders[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]]
) extends Orders[F] {
  import OrderQueries._

  override def get(userId: UserId, orderId: OrderId): F[Option[Order]] =
    sessionPool.use { sn =>
      sn.prepare(selectByUserIdAndOrderId).use { qry =>
        qry.option(userId ~ orderId)
      }
    }

  override def findBy(userId: UserId): F[List[Order]] =
    sessionPool.use { sn =>
      sn.prepare(selectByUserId).use { qry =>
        qry.stream(userId, 1024).compile.toList
      }
    }

  override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): F[OrderId] =
    sessionPool.use { sn =>
      sn.prepare(insertOrder).use { cmd =>
        GenUUID[F].make[OrderId].flatMap { orderId =>
          val itMap = items.map(x => x.item.uuid -> x.quantity).toMap
          val order = Order(orderId, paymentId, itMap, total)
          cmd.execute(userId ~ order).as(orderId)
        }
      }
    }
}

private object OrderQueries {
  val decoder: Decoder[Order] = (
    uuid.cimap[OrderId] ~ uuid ~ uuid.cimap[PaymentId] ~ jsonb[Map[ItemId, Quantity]] ~ numeric.map(USD.apply)
  ).map { case orderId ~ _ ~ paymentId ~ items ~ total =>
    Order(orderId, paymentId, items, total)
  }

  // This additional input of userID is why we don't use a Codec
  val encoder: Encoder[UserId ~ Order] =
    (uuid.cimap[OrderId] ~
      uuid.cimap[UserId] ~
      uuid.cimap[PaymentId] ~
      jsonb[Map[ItemId, Quantity]] ~
      numeric.contramap[Money](_.amount)).contramap { case userId ~ order =>
      order.id ~ userId ~ order.pid ~ order.items ~ order.total
    }

  val insertOrder: Command[UserId ~ Order] =
    sql"""
         INSERT INTO orders
         VALUES ($encoder)
       """.command

  val selectByUserId: Query[UserId, Order] =
    sql"""
         SELECT * FROM orders
         WHERE user_id = ${uuid.cimap[UserId]}
       """.query(decoder)

  val selectByUserIdAndOrderId: Query[UserId ~ OrderId, Order] =
    sql"""
         SELECT * FROM orders
         WHERE user_id = ${uuid.cimap[UserId]}
         AND uuid = ${uuid.cimap[OrderId]}
       """.query(decoder)
}
