package shop

import io.estatico.newtype.Coercible
import org.scalacheck.Arbitrary
import shop.domain.Brand.Brand
import shop.domain.CardModels.Card
import shop.domain.Item.Item
import shop.domain.ShoppingCart.{ Cart, CartTotal }
import shop.generators._

import java.util.UUID

object arbitrary {
  implicit def arbCoercible[A: Coercible[UUID, *]]: Arbitrary[A] = Arbitrary(coerceGenUuid[A])

  implicit val arbCartTotal: Arbitrary[CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCart: Arbitrary[Cart] =
    Arbitrary(cartGen)

  implicit val arbCard: Arbitrary[Card]   = Arbitrary(cardGen)
  implicit val arbBrand: Arbitrary[Brand] = Arbitrary(brandGen)
  implicit val arbItem: Arbitrary[Item]   = Arbitrary(itemGen)
}
