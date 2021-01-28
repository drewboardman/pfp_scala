package shop.config

import cats.effect._
import cats.syntax.all._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration._
import shop.config.Data._
import shop.config.environments.AppEnvironment
import shop.config.environments.AppEnvironment.{Prod, Test}

object loader {
  def apply[F[_]: Async: ContextShift]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case Test =>
          default(
            redisURI = RedisURI("redis://localhost"),
            paymentURI = PaymentURI("http://10.123.154.10/api")
          )
        case Prod =>
          default(
            redisURI = RedisURI("redis://10.123.154.176"),
            paymentURI = PaymentURI("https://payments.net/api")
            )
      }.load[F]

  private def default(
    redisURI: RedisURI,
    paymentURI: PaymentURI
  ): ConfigValue[AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[NonEmptyString].secret,
      env("SC_JWT_CLAIM").as[NonEmptyString].secret,
      env("SC_ACCESS_TOKEN_SECRET_KEY").as[NonEmptyString].secret,
      env("SC_ADMIN_USER_TOKEN").as[NonEmptyString].secret,
      env("SC_PASSWORD_SALT").as[NonEmptyString].secret,
    ).parMapN { (secretKey, claimStr, tokenKey, adminToken, salt) =>
      AppConfig(
        AdminJwtConfig(
          JwtSecretKeyConfig(secretKey),
          JwtClaimConfig(claimStr),
          AdminUserTokenConfig(adminToken)
        ),
        JwtSecretKeyConfig(tokenKey),
        PasswordSalt(salt),
        TokenExpiration(30.minutes),
        ShoppingCartExpiration(30.minutes),
        CheckoutConfig(
          retriesLimit = 3,
          retriesBackoff = 10.milliseconds
          ),
        PaymentConfig(paymentURI),
        HttpClientConfig(
          connectTimeout = 2.seconds,
          requestTimeout = 2.seconds
          ),
        HttpServerConfig(
          host = "0.0.0.0",
          port = 8080
          ),
        RedisConfig(redisURI),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "store",
          max = 10
          )
      )
    }
}
