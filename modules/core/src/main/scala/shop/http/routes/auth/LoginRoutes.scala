package shop.http.routes.auth

import cats.Defer
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import shop.algebras.Auth
import shop.domain.Auth.LoginUser
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    authInterpreter: Auth[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser] { user => // this is handling Validation errors
      }
  }
}
