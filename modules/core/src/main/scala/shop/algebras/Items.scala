package shop.algebras

import shop.domain.Brand.{ Brand, BrandId, BrandName }
import shop.domain.Category.{ Category, CategoryId, CategoryName }
import shop.domain.Item._
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
