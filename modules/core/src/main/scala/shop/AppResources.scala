package shop

import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import io.chrisdavenport.log4cats.Logger
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import dev.profunktor.redis4cats.log4cats._

object AppResources {
  def mkRedisResource[F[_]: ConcurrentEffect: ContextShift: Logger](
      uri: String
  ): Resource[F, RedisCommands[F, String, String]] =
    Redis[F].utf8(uri)
}
