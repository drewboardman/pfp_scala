package shop.http.routes.secured

import cats._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.algebras.Orders
import shop.domain.Orders.OrderId
import shop.http.Json.{ deriveEntityEncoder, orderCodec }
import shop.http.auth.Users.CommonUser

final class OrderRoutes[F[_]: Defer: Monad](
    orders: Orders[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    // get all user orders by userID
    case GET -> Root as commonUser                    =>
      Ok(orders.findBy(commonUser.user.userId))

    // get single order by order UUID
    case GET -> Root / UUIDVar(orderId) as commonUser =>
      Ok(orders.get(commonUser.user.userId, OrderId(orderId)))
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
