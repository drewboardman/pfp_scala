package shop.domain

import shop.domain.Auth.UserId
import squants.market.Money

object Payment {
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )

  // These are placeholder types
  case class Card(
      name: String,
      number: Long,
      expiration: String,
      cvv: Int
  )
}
