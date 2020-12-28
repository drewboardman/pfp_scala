package shop.algebras

import cats._
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.syntax._
import io.circe.parser.decode
import pdi.jwt.JwtClaim
import shop.config.Data.TokenExpiration
import shop.domain.Auth.{ Password, UserName, UserNameInUse }
import shop.effects.CommonEffects.MonadThrow
import shop.effects.GenUUID
import shop.http.Json._
import shop.http.auth.Users.{ AdminUser, CommonUser, User }

trait Auth[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, userName: UserName): F[Unit]
}

object LiveAuth {
  def make[F[_]: Sync](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[LiveAuth[F]] =
    Sync[F].delay(
      new LiveAuth[F](
        tokenExpiration,
        tokens,
        users,
        redis
      )
    )
}

final class LiveAuth[F[_]: GenUUID: MonadThrow] private (
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisCommands[F, String, String]
) extends Auth[F] {
  override def newUser(username: UserName, password: Password): F[JwtToken] =
    users.find(username, password).flatMap {
      case Some(_) =>
        UserNameInUse(username).raiseError[F, JwtToken]
      case None    =>
        for {
          userId <- users.create(username, password)
          token <- tokens.create
          usr = User(userId, usr).asJson.noSpaces // no fallback if this fails
          _ <- redis.setEx(token.value, usr, tokenExpiration.value)
          _ <- redis.setEx(username.value, token.value, tokenExpiration.value) // why do I need this one?
        } yield token
    }
}

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

object LiveUsersAuth {
  def make[F[_]: Sync](
      redis: RedisCommands[F, String, String]
  ): F[LiveUsersAuth[F]] =
    Sync[F].delay(new LiveUsersAuth[F](redis))
}

object LiveAdminAuth {
  def make[F[_]: Sync](
      adminToken: JwtToken,
      adminUser: AdminUser
  ): F[LiveAdminAuth[F]] =
    Sync[F].delay(new LiveAdminAuth[F](adminToken, adminUser))
}

final class LiveUsersAuth[F[_]: Functor] private (
    redis: RedisCommands[F, String, String]
) extends UsersAuth[F, CommonUser] {
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

final class LiveAdminAuth[F[_]: Applicative] private (
    adminToken: JwtToken,
    adminUser: AdminUser
) extends UsersAuth[F, AdminUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    (token == adminToken)
      .guard[Option]
      .as(adminUser)
      .pure[F]
}
