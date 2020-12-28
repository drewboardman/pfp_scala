package shop.http.routes.admin

import cats.Defer
import cats.implicits._
import org.http4s.{ AuthedRoutes, HttpRoutes }
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import shop.algebras.Items
import shop.domain.Item.{ CreateItemParam, ItemAlreadyExists, ItemNotFound, UpdateItemParam }
import shop.effects.CommonEffects.MonadThrow
import shop.http.Decode.RequestPimping
import shop.http.auth.Users.AdminUser
import shop.http.Json._

final class AdminItemsRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    items: Items[F]
) extends Http4sDsl[F] {

  private[admin] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    // create item
    case authReq @ POST -> Root as _ =>
      authReq.req.decodeR[CreateItemParam] {
        cItem =>
          items
            .create(cItem.toCreateItem)
            .flatMap(Created(_))
            .recoverWith {
              case ItemAlreadyExists(itemName, _, _) => // figure out how to use these params in Conflict
                Conflict(itemName.value)
            }
      }

    // update item
    case authReq @ POST -> Root as _ =>
      authReq.req.decodeR[UpdateItemParam] {
        uItem =>
          items
            .update(uItem.toUpdateItem)
            .flatMap(Ok(_))
            .recoverWith {
              case ItemNotFound(itemId) =>
                BadRequest(itemId.value)
            }
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
