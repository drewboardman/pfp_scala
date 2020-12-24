package shop.http

import cats.Applicative
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import shop.domain.Auth.LoginUser
import shop.domain.Brand.{ Brand, BrandParam }
import shop.domain.CardModels._
import shop.domain.Category.Category
import shop.domain.Item.Item
import shop.domain.Orders.{ Order, PaymentId }
import shop.domain.ShoppingCart.{ Cart, CartItem, CartTotal }

// this is used for the card types
import shop.ext.Refined._
import squants.market._

object Json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

private[http] trait JsonCodecs {

  // ----- Overriding some Coercible codecs ----
  implicit val brandParamDecoder: Decoder[BrandParam] =
    Decoder.forProduct1("name")(BrandParam.apply)

  implicit val paymentIdDecoder: Decoder[PaymentId] =
    Decoder.forProduct1("paymentId")(PaymentId.apply)

  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])

  // ----- Domain codecs -----

  implicit val brandDecoder: Decoder[Brand] = deriveDecoder[Brand]
  implicit val brandEncoder: Encoder[Brand] = deriveEncoder[Brand]

  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val itemDecoder: Decoder[Item] = deriveDecoder[Item]
  implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]

  implicit val cartItemDecoder: Decoder[CartItem] = deriveDecoder[CartItem]
  implicit val cartItemEncoder: Encoder[CartItem] = deriveEncoder[CartItem]

  implicit val cartTotalEncoder: Encoder[CartTotal] = deriveEncoder[CartTotal]
  implicit val cartTotalDecoder: Decoder[CartTotal] = deriveDecoder[CartTotal]

  implicit val cardDecoder: Decoder[Card] = deriveDecoder[Card]

  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]

  // gives you both. Look at Codec.AsObject
  implicit val cartCodec: Codec[Cart] =
    Codec.forProduct1(nameA0 = "items")(Cart.apply)(_.items)

  implicit val orderCodec: Codec[Order] =
    Codec.forProduct4(
      "id",
      "pid",
      "items",
      "total"
    )(Order.apply) { out =>
      (
        out.id,
        out.pid,
        out.items,
        out.total
      )
    }
}
