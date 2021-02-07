package shop.effects

import cats.{ ApplicativeError, MonadError }
import cats.effect.Bracket
import cats.mtl.ApplicativeAsk
import shop.config.Data.{ AppConfig, ResourcesConfig }

object CommonEffects {
  type HasAppConfig[F[_]]       = ApplicativeAsk[F, AppConfig]
  type HasResourcesConfig[F[_]] = ApplicativeAsk[F, ResourcesConfig]
  type BracketThrow[F[_]]       = Bracket[F, Throwable]

  object BracketThrow {
    def apply[F[_]](implicit ev: Bracket[F, Throwable]): BracketThrow[F] = ev
  }

  type ApThrow[F[_]] = ApplicativeError[F, Throwable]

  object ApThrow {
    def apply[F[_]](implicit ev: ApplicativeError[F, Throwable]): ApThrow[F] = ev
  }

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }
}
