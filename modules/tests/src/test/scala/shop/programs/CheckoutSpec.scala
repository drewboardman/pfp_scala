package shop.programs

import cats.effect.IO
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import shop.algebras.{ Orders, ShoppingCart }
import shop.arbitrary._
import shop.domain.Auth.UserId
import shop.domain.CardModels.Card
import shop.domain.Item.ItemId
import shop.domain.Orders.{ EmptyCartError, Order, OrderId, PaymentId }
import shop.domain.Payment
import shop.domain.ShoppingCart.{ Cart, CartItem, CartTotal, Quantity }
import shop.effects.Background
import shop.http.clients.PaymentClient
import squants.market.{ Money, USD }
import suite.{ IOAssertion, PureTestSuite }

final class CheckoutSpec extends PureTestSuite {
  val MaxRetries                   = 3
  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successPaymentClient(pid: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      override def process(payment: Payment.Payment): IO[PaymentId] = IO.pure(pid)
    }

  def emptyShoppingCart: TestCart =
    new TestCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(CartTotal(List.empty, USD(0)))
    }

  def successfulShoppingCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new TestCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)

      override def delete(userId: UserId): IO[Unit] = IO.unit
    }

  def successfulOrders(oid: OrderId): TestOrders =
    new TestOrders {
      override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] =
        IO.pure(oid)
    }

  test("successful checkout") {
    implicit val bg: Background[IO] = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](
          successPaymentClient(pid),
          successfulShoppingCart(ct),
          successfulOrders(oid),
          retryPolicy
        )
          .checkout(uid, card)
          .map { orderId =>
            assert(orderId === oid)
          }
      }
    }
  }

  test("empty cart") {
    implicit val bg: Background[IO] = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, oid: OrderId, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](
          successPaymentClient(pid),
          emptyShoppingCart,
          successfulOrders(oid),
          retryPolicy
        ).checkout(uid, card)
          .attempt
          .map {
            case Left(EmptyCartError) =>
              assert(true) // do not used named arguments here. It will fail.
            case _                    =>
              fail("Cart was expected to be empty, but was not.")
          }
      }
    }
  }
}

protected class TestCart() extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ??? // dont need
  def get(userId: UserId): IO[CartTotal]                                = ???
  def delete(userId: UserId): IO[Unit]                                  = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit]              = ??? // dont need
  def update(userId: UserId, cart: Cart): IO[Unit]                      = ??? // dont need
}

protected class TestOrders() extends Orders[IO] {
  override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] = ???
  override def findBy(userId: UserId): IO[List[Order]]                                                        = ???
  override def get(userId: UserId, orderId: OrderId): IO[Option[Order]]                                       = ???
}
