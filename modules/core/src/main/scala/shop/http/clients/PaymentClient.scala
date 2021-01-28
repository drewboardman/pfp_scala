package shop.http.clients

import cats.syntax.all._
import org.http4s.Method.POST
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import shop.config.Data.PaymentConfig
import shop.domain.Orders.{ PaymentError, PaymentId }
import shop.domain.Payment.Payment
import shop.effects.CommonEffects.BracketThrow
import shop.http.Json._

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

final class LivePaymentClient[F[_]: JsonDecoder: BracketThrow](
    cfg: PaymentConfig,
    client: Client[F]
) extends PaymentClient[F]
    with Http4sClientDsl[F] {

  def process(payment: Payment): F[PaymentId] =
    Uri
      .fromString(cfg.uri.value.value + "/payments")
      .liftTo[F]
      .flatMap { uri =>
        POST(payment, uri)
          .flatMap { req =>
            client
              .run(req)
              .use(handlePaymentApiResponse)
          }
      }

  private def handlePaymentApiResponse(response: Response[F]): F[PaymentId] =
    if (response.status == Status.Ok || response.status == Status.Conflict)
      response.asJsonDecode[PaymentId]
    else
      PaymentError(
        cause = Option(response.status.reason).getOrElse("unknown")
      ).raiseError[F, PaymentId]
}
