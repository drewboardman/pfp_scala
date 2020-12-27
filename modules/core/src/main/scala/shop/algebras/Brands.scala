package shop.algebras

import cats.effect.{ Resource, Sync }
import cats.implicits._
import shop.domain.Brand.{ Brand, BrandId, BrandName }
import shop.effects.CommonEffects.BracketThrow
import shop.effects.GenUUID
import skunk._
import skunk.codec.all._
import skunk.implicits._

/**
  * This is an algebra, not a typeclass.
  * Although they look the same, the main difference is that algebras do not need coherence, they
  * can have multiple instances for the same type. This is not checked by the compiler.
  */
trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}

/**
  * The reason this constructor wraps the interpreter in F[_] is because "all
  * creation of mutable state must be suspended in F". The mutable state here
  * is PSQL stuff, but it could be and API call or just a Ref or getting system.time.
  */
object LiveBrands {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Brands[F]] = Sync[F].delay(
    new LiveBrands[F](sessionPool)
  )
}

final class LiveBrands[F[_]: BracketThrow: GenUUID] private (
    sessionPool: Resource[F, Session[F]]
) extends Brands[F] {
  import BrandQueries._

  override def findAll: F[List[Brand]] =
    sessionPool.use(_.execute(selectAll))

  override def create(brandName: BrandName): F[Unit] =
    sessionPool.use { session =>
      session.prepare(insertBrand).use { command =>
        GenUUID[F].make[BrandId].flatMap { brandId =>
          command.execute(Brand(brandId, brandName)).void
        }
      }
    }
}

private object BrandQueries {
// // This is one way to do this, using a custom newtype cimap
//  val codec: Codec[Brand] =
//    (uuid.cimap[BrandId] ~ varchar.cimap[BrandName])
//      .imap {
//        case id ~ name => Brand(id, name)
//      }(brand => brand.uuid ~ brand.name)

  // This is way more straightforward IMO
  val codec: Codec[Brand] = (uuid ~ varchar).imap {
    case id ~ name =>
      Brand(
        BrandId(id),
        BrandName(name)
      )
  }(brand => brand.uuid.value ~ brand.name.value)

  val selectAll: Query[Void, Brand] =
    sql"""
         SELECT * FROM brands
       """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
         INSERT INTO brands
         VALUES ($codec)
       """.command
}
