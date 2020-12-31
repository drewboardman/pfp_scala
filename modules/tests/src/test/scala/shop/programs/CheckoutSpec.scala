package shop.programs

import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.implicits.catsSyntaxTuple2Semigroupal
import io.chrisdavenport.log4cats.Logger
import retry.RetryPolicies.limitRetries
import retry.RetryPolicy
import shop.algebras.{ Orders, ShoppingCart }
import shop.arbitrary._
import shop.domain.Auth.UserId
import shop.domain.CardModels.Card
import shop.domain.Item.ItemId
import shop.domain.Orders._
import shop.domain.Payment.Payment
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
      override def process(payment: Payment): IO[PaymentId] = IO.pure(pid)
    }

  def emptyShoppingCart: TestCart =
    new TestCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(CartTotal(List.empty, USD(0)))
    }

  val unreachablePaymentClient: PaymentClient[IO] =
    new PaymentClient[IO] {
      override def process(payment: Payment): IO[PaymentId] = IO.raiseError(PaymentError("fake test error"))
    }

  def recoveringPaymentClient(attemptsSoFar: Ref[IO, Int], paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      override def process(payment: Payment): IO[PaymentId] =
        attemptsSoFar.get.flatMap {
          case n if n === 1 => IO.pure(paymentId) // after 1 failed attempt, it succeeds
          case _            =>
            attemptsSoFar.update(_ + 1) *> IO.raiseError(
              PaymentError("fake test error")
            ) // this should fail at attempts == 0
        }
    }

  def successfulShoppingCart(cartTotal: CartTotal): ShoppingCart[IO] =
    new TestCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)

      override def delete(userId: UserId): IO[Unit] = IO.unit
    }

  def failingShoppingCart(cartTotal: CartTotal): TestCart =
    new TestCart {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)

      override def delete(userId: UserId): IO[Unit] = IO.raiseError(new Exception(""))
    }

  def successfulOrders(oid: OrderId): TestOrders =
    new TestOrders {
      override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] =
        IO.pure(oid)
    }

  val failingOrders: TestOrders =
    new TestOrders {
      override def create(userId: UserId, paymentId: PaymentId, items: List[CartItem], total: Money): IO[OrderId] =
        IO.raiseError(OrderError("fake test order error"))
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

  test("failing to delete cart does not affect checkout") {
    implicit val bg: Background[IO] = shop.background.NoOp
    import shop.logger.NoOp
    forAll { (uid: UserId, pid: PaymentId, ct: CartTotal, oid: OrderId, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](
          successPaymentClient(pid),
          failingShoppingCart(ct),
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

  test("unreachable payment client") {
    forAll { (uid: UserId, oid: OrderId, card: Card, ct: CartTotal) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          implicit val bg: Background[IO] = shop.background.NoOp
          implicit val logger: Logger[IO] = shop.logger.acc(logs)
          new CheckoutProgram[IO](
            unreachablePaymentClient,
            successfulShoppingCart(ct),
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .attempt
            .flatMap {
              case Left(PaymentError(_)) =>
                logs.get.map {
                  case (x :: xs) =>
                    assert(x.contains("Giving up") && xs.size === MaxRetries)
                  case _         =>
                    fail(s"Expected $MaxRetries retries.")
                }
              case _                     =>
                fail("Expected a payment error.")
            }
        }
      }
    }
  }

  test("failed payment client succeeds after 1 retry") {
    forAll { (uid: UserId, oid: OrderId, pid: PaymentId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          Ref.of[IO, Int](0).flatMap { attempts =>
            implicit val bg: Background[IO] = shop.background.NoOp
            implicit val logger: Logger[IO] = shop.logger.acc(logs)
            new CheckoutProgram[IO](
              recoveringPaymentClient(attempts, pid),
              successfulShoppingCart(ct),
              successfulOrders(oid),
              retryPolicy
            )
              .checkout(uid, card)
              .attempt
              .flatMap {
                case Right(orderId) =>
                  logs.get.map { xs =>
                    assert((orderId === oid && xs.size === 1))
                  }
                case Left(_)        => fail("expected payment id")
              }
          }
        }
      }
    }
  }

  test("cannot create order, run in the background") {
    forAll { (uid: UserId, pid: PaymentId, ct: CartTotal, card: Card) =>
      IOAssertion {
        for {
          attempts <- Ref.of[IO, Int](0)
          logs <- Ref.of[IO, List[String]](List.empty)
          implicit0(bg: Background[IO]) = shop.background.counter(attempts)
          implicit0(logger: Logger[IO]) = shop.logger.acc(logs)
          eitherResult <- new CheckoutProgram[IO](
                            successPaymentClient(pid),
                            successfulShoppingCart(ct),
                            failingOrders,
                            retryPolicy
                          )
                            .checkout(uid, card)
                            .attempt
        } yield eitherResult match {
          case Left(OrderError(_)) =>
            (attempts.get, logs.get).mapN {
              case (count, (x :: y :: xs)) =>
                assert(
                  x.contains("Rescheduling") &&
                  y.contains("Giving up") &&
                  xs.size === MaxRetries &&
                  count === 1
                )
              case _                       => fail(s"Expected $MaxRetries retries and rescheduling")
            }
          case _                   =>
            fail("Expected OrderError")
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
