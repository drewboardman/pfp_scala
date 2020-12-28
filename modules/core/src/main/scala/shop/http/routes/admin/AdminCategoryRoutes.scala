package shop.http.routes.admin

import cats.Defer
import cats.implicits._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.algebras.Categories
import shop.domain.Category.{ CategoryAlreadyExists, CategoryParam }
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.Json._
import shop.http.auth.Users.AdminUser

final class AdminCategoryRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    categories: Categories[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/categories"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of {
      case authReq @ POST -> Root as _ =>
        authReq.req.decodeR[CategoryParam] { cat =>
          categories
            .create(cat.toCategoryName)
            .flatMap(Created(_))
            .recoverWith {
              case CategoryAlreadyExists(category) =>
                Conflict(category.name)
            }
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )
}
