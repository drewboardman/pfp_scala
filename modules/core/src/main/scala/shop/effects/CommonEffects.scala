package shop.effects

import cats.MonadError
import cats.effect.Bracket

object CommonEffects {
  type BracketThrow[F[_]] = Bracket[F, Throwable]

  object BracketThrow {
    def apply[F[_]](implicit ev: Bracket[F, Throwable]): BracketThrow[F] = ev
  }

  type MonadThrow[F[_]] = MonadError[F, Throwable]

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }
}
