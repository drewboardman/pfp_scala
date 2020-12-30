package shop

import eu.timepit.refined.api.Refined
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.scalacheck.Gen
import shop.domain.Brand.{ Brand, BrandId, BrandName }
import shop.domain.CardModels.{
  Card,
  CardCCV,
  CardCCVPred,
  CardExpiration,
  CardExpirationPred,
  CardName,
  CardNamePred,
  CardNumber,
  CardNumberPred
}
import shop.domain.Category.{ Category, CategoryId, CategoryName }
import shop.domain.Item.{ Item, ItemDescription, ItemId, ItemName }
import shop.domain.ShoppingCart.{ CartItem, CartTotal, Quantity }
import squants.market.{ Money, USD }

import java.util.UUID

object generators {

  //------------- helpers -------------------
  val genNonEmptyString: Gen[String] =
    Gen
      .chooseNum(10, 30)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def coerceGenUuid[A: Coercible[UUID, *]]: Gen[A]  = Gen.uuid.map(_.coerce[A])
  def coerceGenStr[A: Coercible[String, *]]: Gen[A] = genNonEmptyString.map(_.coerce[A])
  def coerceGenInt[A: Coercible[Int, *]]: Gen[A]    = Gen.posNum[Int].map(_.coerce[A])

  val genMoney: Gen[Money] =
    Gen.posNum[Long].map { n =>
      USD(BigDecimal(n))
    }

  //---------------- domain ----------------------
  val brandGen: Gen[Brand] = for {
    i <- coerceGenUuid[BrandId]
    n <- coerceGenStr[BrandName]
  } yield Brand(i, n)

  val categoryGen: Gen[Category] = for {
    i <- coerceGenUuid[CategoryId]
    n <- coerceGenStr[CategoryName]
  } yield Category(i, n)

  val itemGen: Gen[Item] = for {
    id <- coerceGenUuid[ItemId]
    name <- coerceGenStr[ItemName]
    desc <- coerceGenStr[ItemDescription]
    p <- genMoney
    b <- brandGen
    c <- categoryGen
  } yield Item(
    uuid = id,
    name = name,
    description = desc,
    price = p,
    brand = b,
    category = c
  )

  val cartItemGen: Gen[CartItem] =
    for {
      itm <- itemGen
      q <- coerceGenInt[Quantity]
    } yield CartItem(itm, q)

  val cartTotalGen: Gen[CartTotal] = for {
    cartItems <- Gen.nonEmptyListOf(cartItemGen)
    ttl <- genMoney
  } yield CartTotal(cartItems, ttl)

  val cardGen: Gen[Card] = for {
    n <- genNonEmptyString.map[CardNamePred](Refined.unsafeApply)
    u <- Gen.posNum[Long].map[CardNumberPred](Refined.unsafeApply)
    x <- Gen.posNum[Int].map[CardExpirationPred] { x =>
           Refined.unsafeApply(x.toString)
         }
    c <- Gen.posNum[Int].map[CardCCVPred](Refined.unsafeApply)
  } yield Card(
    name = CardName(n),
    number = CardNumber(u),
    expiration = CardExpiration(x),
    cvv = CardCCV(c)
  )
}
