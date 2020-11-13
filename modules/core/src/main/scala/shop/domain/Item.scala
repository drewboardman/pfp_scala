package shop.domain

import java.util.UUID

import io.estatico.newtype.macros.newtype
import shop.domain.Brand.{ Brand, BrandId }
import shop.domain.Category.Category
import squants.market.Money

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

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      category: Category
  )

  case class UpdateItem(
      id: ItemId,
      price: Money
  )
}
