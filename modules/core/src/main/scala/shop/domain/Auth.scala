package shop.domain

import java.util.UUID

import io.estatico.newtype.macros.newtype

object Auth {
  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  @newtype case class Password(value: String)
  @newtype case class JwtToken(value: String)
}
