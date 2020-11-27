package shop.domain

import java.util.UUID

import io.estatico.newtype.macros.newtype
import shop.domain.Item.ItemId
import shop.domain.ShoppingCart.Quantity
import squants.market.Money

import scala.util.control.NoStackTrace

object Orders {
  @newtype case class OrderId(uuid: UUID)
  @newtype case class PaymentId(uuid: UUID)

  case class Order(
      id: OrderId,
      pid: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  case object EmptyCartError extends NoStackTrace
  case class OrderError(cause: String) extends NoStackTrace
  case class PaymentError(cause: String) extends NoStackTrace
}
