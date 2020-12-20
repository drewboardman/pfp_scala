package shop.http.routes

import cats.{ Defer, Monad }
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.algebras.Items
import shop.domain.Brand.BrandParam
import shop.http.Json.itemEncoder
import shop.http.Params._

final class ItemRoutes[F[_]: Defer: Monad](
    items: Items[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/items"

  object BrandQueryParam extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    // the ? operator declares this query param optional
    case GET -> Root :? BrandQueryParam(brandToFilterOn) =>
      Ok(brandToFilterOn.fold(items.findAll)(b => items.findBy(b.toBrandName)))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
