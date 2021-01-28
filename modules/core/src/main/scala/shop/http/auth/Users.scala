package shop.http.auth

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import shop.domain.Auth.{ UserId, UserName }
import io.estatico.newtype.macros.newtype

object Users {
  case class User(userId: UserId, userName: UserName)

  @newtype case class CommonUser(user: User)
  @newtype case class AdminUser(user: User)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)
  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
}
