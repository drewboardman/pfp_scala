package shop.programs

import cats.MonadError

import scala.concurrent.duration._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import retry.{ retryingOnAllErrors, RetryDetails, RetryPolicy, Sleep }
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import shop.algebras.{ Orders, PaymentClient, ShoppingCart }
import shop.domain.Auth.UserId
import shop.domain.Orders.{ OrderError, OrderId, PaymentError, PaymentId }
import retry.RetryPolicies._
import shop.domain.Payment.{ Card, Payment }
import shop.domain.ShoppingCart.CartItem
import shop.effects.Background
import squants.market.Money

final class CheckoutProgram[F[_]](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
)(implicit F: MonadError[F, Throwable], l: Logger[F], s: Sleep[F], b: Background[F]) { // cant use context bounding with typeclasses that have multiple type parameters
  private val retryPolicy: RetryPolicy[F] = limitRetries[F](3) |+| exponentialBackoff[F](10.milliseconds)

  def checkout(userId: UserId, card: Card): F[OrderId] =
    for {
      cartTotal <- shoppingCart.get(userId)
      payment = Payment(userId, cartTotal.total, card)
      paymentId <- paymentClient.process(payment)
      orderId <- orders.create(userId, paymentId, cartTotal.items, cartTotal.total)
      _ <- shoppingCart.delete(userId).attempt.void // attempt returns an Either. void discards it (not great)
    } yield orderId

  def processPayment(payment: Payment): F[PaymentId] = {
    val action: F[PaymentId] = retryingOnAllErrors[PaymentId](
      policy = retryPolicy,
      onError = logError("Payments")
    )(paymentClient.process(payment))

    // turns the Throwable in to a PaymentError
    action.adaptError {
      case e =>
        // e.getMessage can be null. Option.apply changes that into a None
        PaymentError(Option(e.getMessage).getOrElse("Unknown: e.getMessage returned null"))
    }
  }

  def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId] = {
    val action = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, items, total))

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.adaptError {
          case e => OrderError(e.getMessage)
        }
        .onError {
          case _ =>
            Logger[F].error(s"Failed to create order for: $paymentId") *> Background[F].schedule(bgAction(fa), 1.hour)
        }

    bgAction(action)
  }

  // this satisfies the retryingOnAllErrors argument of (E, RetryDetails) => M[Unit]
  def logError(thingThatFailed: String)(e: Throwable, details: RetryDetails): F[Unit] = details match {
    case r: WillDelayAndRetry =>
      Logger[F].error(s"Failed on $thingThatFailed with ${e.getMessage}. We retried ${r.retriesSoFar} times.")
    case g: GivingUp =>
      Logger[F].error(
        s"Giving up on $thingThatFailed with ${e.getMessage}. We retried a total of ${g.totalRetries} times."
      )
  }
}
