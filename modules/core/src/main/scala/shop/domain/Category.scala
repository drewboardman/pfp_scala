package shop.domain

import java.util.UUID

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.util.control.NoStackTrace

object Category {
  @newtype case class CategoryId(value: UUID)
  @newtype case class CategoryName(value: String)

  case class Category(uuid: CategoryId, name: CategoryName)

  @newtype case class CategoryParam(value: NonEmptyString) {
    def toCategoryName: CategoryName = CategoryName(value.value.toLowerCase.capitalize)
  }

  case class CategoryAlreadyExists(category: Category) extends NoStackTrace
}
