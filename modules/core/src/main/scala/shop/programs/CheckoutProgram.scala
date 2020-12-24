package shop.programs

import cats.effect.Timer
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.RetryPolicies._
import retry.{ retryingOnAllErrors, RetryDetails, RetryPolicy }
import shop.algebras.{ Orders, PaymentClient, ShoppingCart }
import shop.domain.Auth.UserId
import shop.domain.CardModels.Card
import shop.domain.Orders._
import shop.domain.Payment.Payment
import shop.domain.ShoppingCart.{ CartItem, CartTotal }
import shop.effects.Background
import shop.effects.CommonEffects.MonadThrow
import squants.market.Money

import scala.concurrent.duration._

final class CheckoutProgram[F[_]: MonadThrow: Logger: Timer: Background](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F]
) {
  private val retryPolicy: RetryPolicy[F] = // move this somewhere it can be used more generally
    limitRetries[F](3) |+| exponentialBackoff[F](10.milliseconds)

  def checkout(userId: UserId, card: Card): F[OrderId] =
    shoppingCart
      .get(userId)
      .ensure(EmptyCartError)(_.items.nonEmpty)
      .flatMap {
        case CartTotal(items, total) =>
          val payment = Payment(userId, total, card)
          for {
            paymentId <- paymentClient.process(payment)
            orderId <- orders.create(userId, paymentId, items, total)
            _ <- shoppingCart.delete(userId).attempt.void // attempt returns an Either. void discards it (not great)
          } yield orderId
      }

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
    val action: F[OrderId] = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, items, total))

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.adaptError {
          case e => OrderError(e.getMessage)
        }
        .onError {
          case _ =>
            Logger[F].error(s"Failed to create order for: $paymentId") *>
                Background[F].schedule(bgAction(fa), 1.hour) // wait an hour and then retry
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
