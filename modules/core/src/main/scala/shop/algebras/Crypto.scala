package shop.algebras

import shop.domain.Auth.{ EncryptedPassword, Password }

trait Crypto {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}
