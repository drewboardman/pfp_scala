package shop.http.routes.secured

import cats._
import cats.implicits._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes, Response }
import shop.domain.Orders.{ EmptyCartError, OrderError, PaymentError }
import shop.domain.ShoppingCart.CartNotFound
import shop.domain.Payment.Card
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.Json._
import shop.http.auth.Users.CommonUser
import shop.programs.CheckoutProgram

final class CheckoutRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    checkoutProgram: CheckoutProgram[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case authReq @ POST -> Root as commonUser =>
      authReq.req.decodeR[Card](handleCard(commonUser, _))
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

  private def handleCard(commonUser: CommonUser, card: Card): F[Response[F]] =
    checkoutProgram
      .checkout(commonUser.user.userId, card)
      .flatMap(Created(_))
      .recoverWith { // this is going to change to use classy prisms
        case CartNotFound(userId) => NotFound(s"ShoppingCart not found for user with id: ${userId.value}")
        case EmptyCartError       => BadRequest("ShoppingCart is empty!")
        case PaymentError(cause)  => BadRequest(cause)
        case OrderError(cause)    => BadRequest(cause)
      }
}
