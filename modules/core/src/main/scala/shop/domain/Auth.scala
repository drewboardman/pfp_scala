package shop.domain

import java.util.UUID
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import javax.crypto.Cipher
import scala.util.control.NoStackTrace

object Auth {
  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)

  @newtype case class EncryptedPassword(value: String)
  @newtype case class EncryptCipher(value: Cipher)
  @newtype case class DecryptCipher(value: Cipher)

  //--------- login errors -------------
  case class InvalidUserOrPassword(username: UserName) extends NoStackTrace
  case class UserNameInUse(username: UserName) extends NoStackTrace

  //--------- registering users -------------
  @newtype case class UserNameParam(value: NonEmptyString) {
    def toUserName: UserName = UserName(value.value.toLowerCase)
  }

  @newtype case class PasswordParam(value: NonEmptyString) {
    def toPassword: Password = Password(value.value)
  }

  //--------- create a user    --------------
  case class CreateUser(
      username: UserNameParam,
      password: PasswordParam
  )

  //--------- logging in users --------------
  case class LoginUser(
      userName: UserNameParam,
      password: PasswordParam
  )
}
