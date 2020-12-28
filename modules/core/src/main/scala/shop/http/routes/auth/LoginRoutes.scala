package shop.http.routes.auth

import cats.Defer
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.algebras.Auth
import shop.domain.Auth.{ InvalidUserOrPassword, LoginUser }
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.Json._

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    authInterpreter: Auth[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser] { user => // this is handling Validation errors
        authInterpreter
          .login(user.userName.toUserName, user.password.toPassword)
          .flatMap(Ok(_))
          .recoverWith {
            case InvalidUserOrPassword(_) =>
              Forbidden()
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes // no middleware?
  )
}
