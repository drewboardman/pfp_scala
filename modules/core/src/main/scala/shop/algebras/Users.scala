package shop.algebras

import shop.domain.Auth.{ Password, UserId, UserName }
import shop.http.auth.Users.User

trait Users[F[_]] {
  def find(
      username: UserName,
      password: Password
  ): F[Option[User]]

  def create(
      username: UserName,
      password: Password
  ): F[UserId]
}
