package shop.algebras

import cats.effect.{ Resource, Sync }
import cats.implicits._
import shop.domain.Brand.{ Brand, BrandId, BrandName }
import shop.domain.Category.{ Category, CategoryId, CategoryName }
import shop.domain.Item._
import shop.effects.GenUUID
import shop.ext.skunkx.CodecOps
import skunk._
import skunk.codec.all._
import skunk.implicits._
import squants.market.USD

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findBy(brand: BrandName): F[List[Item]]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: UpdateItem): F[Unit]
}

object LiveItems {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Items[F]] = Sync[F].delay(
    new LiveItems[F](sessionPool)
  )
}

final class LiveItems[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]]
) extends Items[F] {
  import ItemQueries._

  // this could be changed to return a stream
  override def findAll: F[List[Item]] =
    sessionPool.use(_.execute(selectAll))

  override def findBy(brand: BrandName): F[List[Item]] = sessionPool.use {
    session =>
      session.prepare(selectByBrand).use {
        prepared =>
          prepared.stream(args = brand, chunkSize = 1024).compile.toList
      }
  }

  override def findById(itemId: ItemId): F[Option[Item]] = sessionPool.use {
    session =>
      session.prepare(selectByItemId).use {
        prepared =>
          prepared.option(itemId)
      }
  }

  override def create(item: CreateItem): F[Unit] = sessionPool.use {
    session =>
      session.prepare(insertItem).use {
        cmd =>
          GenUUID[F].make[ItemId].flatMap {
            itemId =>
              cmd.execute(itemId ~ item).void
          }
      }
  }

  override def update(item: UpdateItem): F[Unit] = sessionPool.use {
    session =>
      session.prepare(updateItem).use {
        cmd =>
          cmd.execute(item).void
      }
  }
}

private object ItemQueries {
  val decoder: Decoder[Item] =
    (uuid ~ varchar ~ varchar ~ numeric ~ uuid ~ varchar ~ uuid ~ varchar).map {
      case uuid ~ name ~ description ~ price ~ brandId ~ brandName ~ catId ~ catName =>
        Item(
          uuid = ItemId(uuid),
          name = ItemName(name),
          description = ItemDescription(description),
          price = USD(price),
          brand = Brand(BrandId(brandId), BrandName(brandName)),
          category = Category(CategoryId(catId), CategoryName(catName))
        )
    }

  val selectByBrand: Query[BrandName, Item] =
    sql"""
      SELECT
      i.uuid,
      i.name,
      i.description,
      i.price,
      b.uuid,
      b.name,
      c.uuid,
      c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON c.category_id = c.uuid
      WHERE b.name LIKE ${varchar.cimap[BrandName]}
      """.query(decoder)

  val updateItem: Command[UpdateItem] =
    sql"""
         UPDATE items
         SET price = $numeric
         WHERE uuid = ${uuid.cimap[ItemId]}
       """.command.contramap {
      item =>
        item.price.amount ~ item.id
    }

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
         INSERT INTO items
         VALUES ($uuid, $varchar, $varchar, $numeric, $uuid, $uuid)
       """.command.contramap {
      case itemId ~ item =>
        itemId.value ~
          item.name.value ~
          item.description.value ~
          item.price.amount ~
          item.brandId.value ~
          item.categoryId.value
    }

  val selectByItemId: Query[ItemId, Item] =
    sql"""
      SELECT
      i.uuid,
      i.name,
      i.description,
      i.price,
      b.uuid,
      b.name,
      c.uuid,
      c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON c.category_id = c.uuid
      WHERE i.uuid LIKE ${uuid.cimap[ItemId]}
      """.query(decoder)

  val selectAll: Query[Void, Item] =
    sql"""
      SELECT
      i.uuid,
      i.name,
      i.description,
      i.price,
      b.uuid,
      b.name,
      c.uuid,
      c.name
      FROM items AS i
      INNER JOIN brands AS b ON i.brand_id = b.uuid
      INNER JOIN categories AS c ON i.category_id = c.uuid
      """.query(decoder)
}
