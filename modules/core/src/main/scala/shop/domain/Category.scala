package shop.domain

import java.util.UUID

import io.estatico.newtype.macros.newtype

object Category {
  @newtype case class CategoryId(value: UUID)
  @newtype case class CategoryName(value: String)

  case class Category(uuid: CategoryId, name: CategoryName)
}
