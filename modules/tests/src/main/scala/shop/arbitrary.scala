package shop

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops.toCoercibleIdOps
import org.scalacheck.{ Arbitrary, Gen }
import shop.domain.Brand.Brand
import shop.domain.CardModels.Card
import shop.domain.Category.Category
import shop.domain.Item.Item
import shop.domain.ShoppingCart.{ Cart, CartItem, CartTotal }
import shop.generators._
import squants.Money

import java.util.UUID

object arbitrary {
  implicit def arbCoercibleInt[A: Coercible[Int, *]]: Arbitrary[A] = Arbitrary(Gen.posNum[Int].map(_.coerce[A]))

  implicit def arbCoercibleStr[A: Coercible[String, *]]: Arbitrary[A] = Arbitrary(coerceGenStr[A])

  implicit def arbCoercibleUUID[A: Coercible[UUID, *]]: Arbitrary[A] = Arbitrary(coerceGenUuid[A])

  implicit val arbCartTotal: Arbitrary[CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCart: Arbitrary[Cart] =
    Arbitrary(cartGen)

  implicit val arbCard: Arbitrary[Card]         = Arbitrary(cardGen)
  implicit val arbBrand: Arbitrary[Brand]       = Arbitrary(brandGen)
  implicit val arbCategory: Arbitrary[Category] = Arbitrary(categoryGen)
  implicit val arbItem: Arbitrary[Item]         = Arbitrary(itemGen)
  implicit val arbCartItem: Arbitrary[CartItem] = Arbitrary(cartItemGen)
  implicit val arbMoney: Arbitrary[Money]       = Arbitrary(genMoney)
}
