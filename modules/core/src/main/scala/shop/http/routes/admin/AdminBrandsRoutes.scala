package shop.http.routes.admin

import cats.Defer
import cats.implicits._
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import shop.algebras.Brands
import shop.domain.Brand.{ BrandAlreadyExists, BrandParam }
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.auth.Users.AdminUser
import shop.http.Json._

final class AdminBrandsRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    brands: Brands[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case authRequest @ POST -> Root as _ =>
      authRequest.req.decodeR[BrandParam] {
        brandParam =>
          brands
            .create(brandParam.toBrandName)
            .flatMap(Created(_))
            .recoverWith {
              case BrandAlreadyExists(bName) =>
                Conflict(bName.value)
            }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
