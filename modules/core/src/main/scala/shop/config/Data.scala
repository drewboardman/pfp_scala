package shop.config

import ciris.Secret
import eu.timepit.refined.types.all.{ PosInt, UserPortNumber }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object Data {
  @newtype case class PasswordSalt(value: Secret[NonEmptyString])
  @newtype case class ShoppingCartExpiration(value: FiniteDuration)
  @newtype case class TokenExpiration(value: FiniteDuration)
  @newtype case class AdminUserTokenConfig(value: Secret[NonEmptyString])
  @newtype case class JwtSecretKeyConfig(value: Secret[NonEmptyString])
  @newtype case class JwtClaimConfig(value: Secret[NonEmptyString])
  @newtype case class PaymentURI(value: NonEmptyString)
  @newtype case class PaymentConfig(uri: PaymentURI)
  @newtype case class RedisURI(value: NonEmptyString)
  @newtype case class RedisConfig(uri: RedisURI)

  case class AppConfig(
      adminJwtConfig: AdminJwtConfig,
      tokenConfig: JwtSecretKeyConfig,
      passwordSalt: PasswordSalt,
      tokenExpiration: TokenExpiration,
      cartExpiration: ShoppingCartExpiration,
      checkoutConfig: CheckoutConfig,
      paymentConfig: PaymentConfig,
      httpServerConfig: HttpServerConfig,
      resourcesConfig: ResourcesConfig
  )

  case class ResourcesConfig(
      httpClientConfig: HttpClientConfig,
      redis: RedisConfig,
      postgreSQL: PostgreSQLConfig
  )

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: UserPortNumber,
      user: NonEmptyString,
      database: NonEmptyString,
      max: PosInt
  )

  case class HttpClientConfig(
      connectTimeout: FiniteDuration,
      requestTimeout: FiniteDuration
  )

  case class HttpServerConfig(
      host: NonEmptyString,
      port: UserPortNumber
  )

  case class CheckoutConfig(
      retriesLimit: PosInt,
      retriesBackoff: FiniteDuration
  )

  case class AdminJwtConfig(
      secretKeyConfig: JwtSecretKeyConfig,
      claimStr: JwtClaimConfig,
      adminToken: AdminUserTokenConfig
  )
}
