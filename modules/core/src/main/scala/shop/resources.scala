package shop

import cats.FlatMap
import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, Resource }
import cats.implicits._
import cats.mtl.ApplicativeAsk
import com.olegpy.meow.hierarchy._
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import io.chrisdavenport.log4cats.Logger
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import shop.config.Data.{ AppConfig, HttpClientConfig, PostgreSQLConfig, RedisConfig, ResourcesConfig }
import shop.effects.CommonEffects.{ HasAppConfig, HasResourcesConfig }
import skunk.{ Session, SessionPool }

import scala.concurrent.ExecutionContext

final case class AppResources[F[_]](
    client: Client[F],
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
)

object AppResources {
  def make[F[_]: ConcurrentEffect: ContextShift: HasResourcesConfig: Logger]( // need hierarchy for this to work
      ex: ExecutionContext
  ): Resource[F, AppResources[F]] =
    for {
      h <-
        Resource.liftF(
          ApplicativeAsk[F, ResourcesConfig].reader(_.httpClientConfig) // same as ask.map(_.httpClientConfig)
        )
      p <- Resource.liftF(ApplicativeAsk[F, ResourcesConfig].reader(_.postgreSQL))
      r <- Resource.liftF(ApplicativeAsk[F, ResourcesConfig].reader(_.redis))
      httpClient <- mkHttpClientResource(h, ex)
      psql <- mkPostgreSQLResource(p)
      redis <- mkRedisResource(r)
    } yield AppResources.apply[F](httpClient, psql, redis)

  def loadResources[F[_]: ConcurrentEffect: ContextShift: FlatMap: HasAppConfig: Logger](
      ex: ExecutionContext
  )(
      fa: AppConfig => AppResources[F] => F[ExitCode]
  ): F[ExitCode] =
    ApplicativeAsk[F, AppConfig].ask
      .flatMap { cfg =>
        Logger[F].info(s"Loaded config $cfg") >>
          AppResources.make[F](ex).use(res => fa(cfg)(res))
      }

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
