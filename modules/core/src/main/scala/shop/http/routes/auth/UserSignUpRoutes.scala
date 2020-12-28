package shop.http.routes.auth

import cats.Defer
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.algebras.Auth
import shop.domain.Auth.{ CreateUser, UserNameInUse }
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.Json._

final class UserSignUpRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    authInterpreter: Auth[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "users" =>
      request.decodeR[CreateUser] {
        createUser =>
          authInterpreter
            .newUser(createUser.username.toUserName, createUser.password.toPassword)
            .flatMap(Created(_))
            .recoverWith {
              case UserNameInUse(username) =>
                Conflict(username.value)
            }
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
