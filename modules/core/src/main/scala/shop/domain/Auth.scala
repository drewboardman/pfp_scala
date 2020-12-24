package shop.domain

import java.util.UUID

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object Auth {
  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  // i think this goes away
  @newtype case class JwtToken(value: String)

  //--------- registering users -------------
  @newtype case class UserNameParam(value: NonEmptyString) {
    def toUserName: UserName = UserName(value.value.toLowerCase)
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toPassword: Password = Password(value.value)
  }

  //--------- logging in users --------------
  case class LoginUser(
      userName: UserNameParam,
      password: PasswordParam
  )
}
