package shop.http.routes.secured

import cats.data.Kleisli
import cats.effect.IO
import org.http4s._
import org.http4s.Method._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.AuthMiddleware
import shop.algebras.ShoppingCart
import shop.domain.Auth.{ UserId, UserName }
import shop.domain.Item.ItemId
import shop.domain.ShoppingCart
import shop.domain.ShoppingCart.{ Cart, CartTotal, Quantity }
import shop.http.auth.Users.{ CommonUser, User }
import squants.market.USD
import suite.{ HttpTestSuite, IOAssertion }
import shop.arbitrary.{ arbCart, arbCartTotal }
import shop.http.Json._

import java.util.UUID

class CartRoutesSpec extends HttpTestSuite {
  val authUser: CommonUser = CommonUser(User(UserId(UUID.randomUUID), UserName("testuser")))

  val authMiddleware: AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser)) // function that just returns the input (_ => IO.pure(x))

  def dataShoppingCart(cartTotal: CartTotal): TestShoppingCart =
    new TestShoppingCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
    }

  test("GET shopping cart [OK]") {
    forAll { (cartTotal: CartTotal) =>
      IOAssertion {
        val routes: HttpRoutes[IO] = new CartRoutes[IO](dataShoppingCart(cartTotal)).routes(authMiddleware)
        val request: Request[IO]   = Request[IO](GET, uri"/cart")
        assertHttp(routes, request)(Status.Ok, cartTotal)
      }
    }
  }

  test("POST add item to cart [OK]") {
    forAll { (cart: Cart) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/cart").withEntity(cart)
        val routes               = new CartRoutes[IO](new TestShoppingCart).routes(authMiddleware)
        assertHttpStatus(routes, request)(Status.Created)
      }
    }
  }
}

protected class TestShoppingCart extends ShoppingCart[IO] {
  override def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  override def delete(userId: UserId): IO[Unit]                                  = IO.unit
  override def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = IO.unit
  override def update(userId: UserId, cart: ShoppingCart.Cart): IO[Unit]         = IO.unit
  override def get(userId: UserId): IO[CartTotal]                                = IO.pure(CartTotal(List.empty, USD(0)))
}
