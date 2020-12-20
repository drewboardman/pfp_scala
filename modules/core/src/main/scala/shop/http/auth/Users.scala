package shop.http.auth

import shop.domain.Auth.{ UserId, UserName }
import io.estatico.newtype.macros.newtype

object Users {
  case class User(userId: UserId, userName: UserName)

  @newtype case class CommonUser(user: User)
}
