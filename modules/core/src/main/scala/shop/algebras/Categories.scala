package shop.algebras

import cats.effect.{ Resource, Sync }
import cats.implicits._
import shop.domain.Category.{ Category, CategoryId, CategoryName }
import shop.effects.CommonEffects.BracketThrow
import shop.effects.GenUUID
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[Unit]
}

object LiveCategories {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Categories[F]] = Sync[F].delay(
    new LiveCategories[F](sessionPool)
  )
}

final class LiveCategories[F[_]: BracketThrow: GenUUID] private (
    sessionPool: Resource[F, Session[F]]
) extends Categories[F] {
  import CategoryQueries._

  override def findAll: F[List[Category]] =
    sessionPool.use(_.execute(selectAll))

  override def create(name: CategoryName): F[Unit] =
    sessionPool
      .use { session =>
        session.prepare(insertCategory).use { cmd =>
          GenUUID[F].make[CategoryId].flatMap { uuid =>
            cmd.execute(Category(uuid, name)).void
          }
        }
      }
}

private object CategoryQueries {
  val codec: Codec[Category] =
    (uuid ~ varchar).imap {
      case (id ~ name) =>
        Category(
          CategoryId(id),
          CategoryName(name)
        )
    }(category => category.uuid.value ~ category.name.value)

  val selectAll: Query[Void, Category] =
    sql"""
      SELECT * FROM categories
       """.query(codec)

  val insertCategory: Command[Category] =
    sql"""
      INSERT INTO categories
      VALUES ($codec)
       """.command
}
