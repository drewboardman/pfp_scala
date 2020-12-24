package shop.http

import cats.syntax.all._
import io.circe.Decoder
import org.http4s.circe.{ toMessageSynax, JsonDecoder }
import org.http4s.dsl.Http4sDsl
import org.http4s.{ Request, Response }
import shop.effects.CommonEffects.MonadThrow

object Decode {
  implicit class RequestPimping[F[_]: JsonDecoder: MonadThrow](
      request: Request[F]
  ) extends Http4sDsl[F] {

    // This deals with Refined errors on the Card type Validated stuff.
    // Converts the default response of 422 Unprocessable Entity to Bad Request
    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      request
        .asJsonDecode[A]
        .attempt // converts to F[Either[Throwable, A]]
        .flatMap {
          case Left(err) =>
            Option(err.getCause) match {
              case Some(cause) if cause.getMessage.startsWith("Predicate") =>
                BadRequest(cause.getMessage)
              case _ =>
                UnprocessableEntity()
            }
          case Right(a) => f(a)
        }
  }
}
