package shop.http.routes.secured

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.algebras.ShoppingCart
import shop.domain.Item._
import shop.domain.ShoppingCart._
import shop.http.Json.{ cartCodec, cartTotalEncoder, deriveEntityEncoder }
import shop.http.auth.Users.CommonUser

final class CartRoutes[F[_]: Defer: JsonDecoder: Monad](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      // Get shopping cart
      case GET -> Root as commonUser =>
        Ok(shoppingCart.get(commonUser.user.userId))

      // Add items to shopping cart
      case authedReq @ POST -> Root as commonUser =>
        authedReq.req.asJsonDecode[Cart].flatMap { cart =>
          cart.items // cart.items: Map[ItemId, Quantity]
          .toList
            .traverse_ { // I have no idea why this is indenting but it's dumb
              case (itemId, quantity) =>
                shoppingCart.add(commonUser.user.userId, itemId, quantity)
            } *>
            Created()
        }

      // Modify the cart
      case authedReq @ PUT -> Root as commonUser =>
        authedReq.req.asJsonDecode[Cart].flatMap { cart =>
          shoppingCart.update(commonUser.user.userId, cart) *>
            Ok()
        }

      // Remove items from cart
      case DELETE -> Root / UUIDVar(uuid) as commonUser =>
        shoppingCart.removeItem(commonUser.user.userId, ItemId(uuid)) *>
            NoContent()
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
