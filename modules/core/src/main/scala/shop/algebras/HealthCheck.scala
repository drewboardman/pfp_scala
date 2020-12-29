package shop.algebras

import cats.Parallel
import cats.effect.syntax.all._
import cats.effect.{ Concurrent, Resource, Sync, Timer }
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import shop.domain.HealthCheck.{ AppStatus, PostgresStatus, RedisStatus }
import skunk.{ Session, _ }
import skunk.codec.all._
import skunk.implicits._

import scala.concurrent.duration.DurationInt

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object LiveHealthCheck {
  def make[F[_]: Concurrent: Parallel: Timer](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] =
    Sync[F].delay(
      new LiveHealthCheck[F](sessionPool, redis)
    )
}

final class LiveHealthCheck[F[_]: Concurrent: Parallel: Timer](
    sessionPool: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthCheck[F] {
  import HealthCheckQueries._

  private val redisHealth: F[RedisStatus] = redis.ping
    .map(_.nonEmpty)
    .timeout(1.second)
    .orElse(false.pure[F])
    .map(RedisStatus.apply)

  val postgresHealth: F[PostgresStatus] = sessionPool
    .use(_.execute(getPsqlHealth))
    .map(_.nonEmpty)
    .timeout(1.second)
    .orElse(false.pure[F])
    .map(PostgresStatus.apply)

  // performs them both in parallel. F has Concurrent and Parallel instances
  override def status: F[AppStatus] = (redisHealth, postgresHealth).parMapN(AppStatus)
}

object HealthCheckQueries {
  val getPsqlHealth: Query[Void, Int] =
    sql"""
         SELECT pid FROM pg_stat_activity
       """.query(int4)
}
