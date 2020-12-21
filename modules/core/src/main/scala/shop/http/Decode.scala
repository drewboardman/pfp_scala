package shop.http

import cats.data.ReaderT
import io.circe.Decoder
import org.http4s.{ Request, Response }
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import shop.effects.CommonEffects.MonadThrow

object Decode {
  implicit class RequestPimping[F[_]: JsonDecoder: MonadThrow](
      request: Request[F]
  ) extends Http4sDsl[F] {
    def decodeR[A: Decoder](
        f: ReaderT[F, A, Response[F]]
    )
  }
}
