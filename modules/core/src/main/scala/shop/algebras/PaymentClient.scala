package shop.algebras

import shop.domain.Orders.PaymentId
import shop.domain.Payment.Payment

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
