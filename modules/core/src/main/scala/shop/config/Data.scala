package shop.config

import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object Data {
  @newtype case class ShoppingCartExpiration(value: FiniteDuration)
  @newtype case class TokenExpiration(value: FiniteDuration)
}
