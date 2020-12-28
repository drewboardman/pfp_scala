package shop.domain

import io.estatico.newtype.macros.newtype

object HealthCheck {
  @newtype case class RedisStatus(value: Boolean)
  @newtype case class PostgresStatus(value: Boolean)

  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}
