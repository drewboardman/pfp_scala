package shop.domain

import shop.domain.Auth.UserId
import shop.domain.CardModels.Card
import squants.market.Money

object Payment {
  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )
}
