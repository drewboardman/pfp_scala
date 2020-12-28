package shop.domain

import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import shop.domain.Brand.{ Brand, BrandId }
import shop.domain.Category.{ Category, CategoryId }
import squants.market._

import scala.util.control.NoStackTrace

object Item {

  @newtype case class ItemId(value: UUID)
  @newtype case class ItemName(value: String)
  @newtype case class ItemDescription(value: String)

  case class Item(
      uuid: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: Brand,
      category: Category
  )

  //------------- creating an item ---------------
  @newtype case class ItemNameParam(value: NonEmptyString)
  @newtype case class ItemDescriptionParam(value: NonEmptyString)
  @newtype case class PriceParam(value: String Refined ValidBigDecimal)

  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: PriceParam,
      brandId: BrandId,
      categoryId: CategoryId
  ) {
    def toCreateItem: CreateItem =
      CreateItem(
        name = ItemName(name.value.value),
        description = ItemDescription(description.value.value),
        price = USD(BigDecimal(price.value.value)),
        brandId = brandId,
        categoryId = categoryId
      )
  }

  case class ItemAlreadyExists(
      name: ItemName,
      brandId: BrandId,
      categoryId: CategoryId
  ) extends NoStackTrace

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  //------------------ update an item -------------------
  @newtype case class ItemIdParam(value: String Refined Uuid)

  case class UpdateItem(
      id: ItemId,
      price: Money
  )

  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toUpdateItem: UpdateItem =
      UpdateItem(
        id = ItemId(UUID.fromString(id.value.value)),
        price = USD(BigDecimal(price.value.value))
      )
  }

  case class ItemNotFound(itemId: ItemId) extends NoStackTrace
}
