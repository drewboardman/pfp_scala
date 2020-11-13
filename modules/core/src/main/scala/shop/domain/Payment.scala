package shop.domain

import org.http4s.headers.Date
import shop.domain.Auth.UserId
import squants.market.Money

object Payment {
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )

  case class Card(
      name: String,
      number: Long,
      expiration: Date,
      cvv: Int
  )
}
