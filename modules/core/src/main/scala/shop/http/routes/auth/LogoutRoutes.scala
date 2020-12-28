package shop.http.routes.auth

import cats.{ Defer, _ }
import cats.implicits.{ toFoldableOps, _ }
import dev.profunktor.auth.AuthHeaders
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.algebras.Auth
import shop.http.auth.Users.CommonUser

final class LogoutRoutes[F[_]: Defer: Monad](
    authInterpreter: Auth[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case authReq @ POST -> Root / "logout" as commonUser =>
        AuthHeaders
          .getBearerToken(authReq.req)
          .traverse_ { jwt =>
            authInterpreter.logout(jwt, commonUser.user.userName)
          } *> NoContent()
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )
}
