package shop.algebras

import cats.effect.{ Resource, Sync }
import cats.implicits._
import shop.domain.Auth._
import shop.effects.GenUUID
import shop.ext.skunkx.CodecOps
import shop.http.auth.Users.User
import skunk._
import skunk.codec.all._
import skunk.implicits._

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

object LiveUsers {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]],
      crypto: Crypto
  ): F[LiveUsers[F]] = Sync[F].delay(
    new LiveUsers[F](sessionPool, crypto)
  )
}

final class LiveUsers[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]],
    crypto: Crypto
) extends Users[F] {
  import UserQueries._

  override def find(username: UserName, password: Password): F[Option[User]] =
    sessionPool.use { sn =>
      sn.prepare(selectUser).use { qry =>
        qry.option(username).map {
          case Some(user ~ encryptedFromDB) if encryptedFromDB.value == crypto.encrypt(password).value =>
            user.some
          case _                                                                                       =>
            none[User]
        }
      }
    }

  override def create(username: UserName, password: Password): F[UserId] = sessionPool.use { sn =>
    sn.prepare(insertUser).use { cmd =>
      GenUUID[F].make[UserId].flatMap { userId =>
        cmd
          .execute(User(userId, username) ~ crypto.encrypt(password))
          .as(userId)
          .handleErrorWith { case SqlState.UniqueViolation(_) =>
            UserNameInUse(username).raiseError[F, UserId]
          }
      }
    }
  }
}

private object UserQueries {
  val codec: Codec[User ~ EncryptedPassword] =
    (uuid.cimap[UserId] ~
      varchar.cimap[UserName] ~
      varchar.cimap[EncryptedPassword]).imap { case userId ~ username ~ pass =>
      User(userId, username) ~ pass
    } { case user ~ pass =>
      user.userId ~ user.userName ~ pass
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
         SELECT * FROM users
         WHERE name = ${varchar.cimap[UserName]}
       """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
         INSERT INTO users
         VALUES ($codec)
       """.command
}
