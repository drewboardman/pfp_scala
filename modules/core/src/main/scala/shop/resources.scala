package shop

import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import io.chrisdavenport.log4cats.Logger
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import shop.config.Data.{ AppConfig, HttpClientConfig, PostgreSQLConfig, RedisConfig }
import skunk.{ Session, SessionPool }

import scala.concurrent.ExecutionContext

final case class AppResources[F[_]](
    client: Client[F],
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
)

object AppResources {
  def make[F[_]: ConcurrentEffect: ContextShift: Logger](
      cfg: AppConfig,
      ex: ExecutionContext
  ): Resource[F, AppResources[F]] =
    for {
      psql <- mkPostgreSQLResource(cfg.postgreSQL)
      redis <- mkRedisResource(cfg.redis)
      httpClient <- mkHttpClientResource(cfg.httpClientConfig, ex)
    } yield AppResources.apply[F](httpClient, psql, redis)

  private def mkPostgreSQLResource[F[_]: ConcurrentEffect: ContextShift](
      c: PostgreSQLConfig
  ): SessionPool[F] =
    Session
      .pooled[F](
        host = c.host.value,
        port = c.port.value,
        user = c.user.value,
        database = c.database.value,
        max = c.max.value
      )

  private def mkRedisResource[F[_]: ConcurrentEffect: ContextShift: Logger](
      c: RedisConfig
  ): Resource[F, RedisCommands[F, String, String]] = Redis[F].utf8(c.uri.value.value)

  private def mkHttpClientResource[F[_]: ConcurrentEffect: ContextShift: Logger](
      c: HttpClientConfig,
      ex: ExecutionContext
  ): Resource[F, Client[F]] =
    BlazeClientBuilder[F](ex)
      .withConnectTimeout(c.connectTimeout)
      .withRequestTimeout(c.requestTimeout)
      .resource
}
