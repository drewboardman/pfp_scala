package shop.http.routes.secured

import cats._
import org.http4s.AuthedRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import shop.domain.Payment.Card
import cats.syntax.all._
import org.http4s._
import shop.effects.CommonEffects.MonadThrow
import shop.http.auth.Users.CommonUser
import shop.programs.CheckoutProgram

final class CheckoutRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    checkoutProgram: CheckoutProgram[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case authReq @ POST -> Root as commonUser =>
      authReq.req.decodeR[Card] { card =>
      }
  }
}
