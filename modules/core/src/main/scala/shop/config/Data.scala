package shop.config

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object Data {
  @newtype case class PasswordSalt(value: Secret[NonEmptyString])
  @newtype case class ShoppingCartExpiration(value: FiniteDuration)
  @newtype case class TokenExpiration(value: FiniteDuration)
}
