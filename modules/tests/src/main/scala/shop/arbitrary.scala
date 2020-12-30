package shop

import io.estatico.newtype.Coercible
import org.scalacheck.Arbitrary
import shop.domain.CardModels.Card
import shop.domain.ShoppingCart
import shop.generators._

import java.util.UUID

object arbitrary {
  implicit def arbCoercible[A: Coercible[UUID, *]]: Arbitrary[A] = Arbitrary(coerceGenUuid[A])

  implicit val arbCartTotal: Arbitrary[ShoppingCart.CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCard: Arbitrary[Card] = Arbitrary(cardGen)
}
