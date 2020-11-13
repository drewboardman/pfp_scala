package shop.algebras

import shop.domain.Auth.UserId
import shop.domain.Item.ItemId
import shop.domain.ShoppingCart.{ Cart, Quantity }

trait ShoppingCart[F[_]] {
  def add(
      userId: UserId,
      itemId: ItemId,
      quantity: Quantity
  ): F[Unit]

  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}
