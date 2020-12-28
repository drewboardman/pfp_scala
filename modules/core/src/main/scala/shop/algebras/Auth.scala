package shop.algebras

import cats._
import cats.implicits._
import cats.syntax._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import pdi.jwt.JwtClaim
import shop.domain.Auth.{ Password, UserName }
import shop.http.auth.Users.{ CommonUser, User }
import io.circe.syntax._
import shop.http.Json._
import io.circe.parser.decode

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, userName: UserName): F[Unit]
}

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

class LiveUsersAuth[F[_]: Functor](redis: RedisCommands[F, String, String]) extends UsersAuth[F, CommonUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    redis
      .get(token.value)
      .map { maybeUser =>
        for {
          rawUserString <- maybeUser
          user <- decode[User](rawUserString).toOption
        } yield CommonUser(user)
      }
}
