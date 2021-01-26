package shop.algebras

import cats.effect.{IO, Resource}
import cats.implicits.{catsSyntaxEq => _, _}
import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import natchez.Trace.Implicits.noop
import eu.timepit.refined.auto._
import io.estatico.newtype.ops._
import eu.timepit.refined.cats._
import shop.arbitrary.{arbBrand, arbCategory, arbCoercibleStr, arbItem, arbMoney, arbCartItem, arbCoercibleUUID}
import shop.config.Data.PasswordSalt
import shop.domain.Auth.{Password, UserName}
import shop.domain.Brand.{Brand, BrandId}
import shop.domain.Category.{Category, CategoryId}
import shop.domain.Item.{CreateItem, Item}
import shop.domain.Orders.{OrderId, PaymentId}
import shop.domain.ShoppingCart.CartItem
import skunk.Session
import suite.{IOAssertion, ResourceSuite}
import shop.domain._
import squants.Money

class PostgreSQLTest extends ResourceSuite[Resource[IO, Session[IO]]] {
  // this does not actually spin up a running psql
  // we use docker-compose during our tests to ensure that
  override def resources: Resource[IO, Resource[IO, Session[IO]]] =
    Session.pooled[IO](
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "store",
      max = 10
      )

  val MaxTests: PropertyCheckConfigParam = MinSuccessful(1)

  lazy val salt: PasswordSalt = Secret("53kr3t": NonEmptyString).coerce[PasswordSalt]

  withResources { sessionPool =>

    test("Orders") {
      forAll(MaxTests) { (oid: OrderId, pid: PaymentId, uName: UserName, pass: Password, items: List[CartItem], price: Money) =>
        IOAssertion {
          for {
            c <- LiveCrypto.make[IO](salt)
            u <- LiveUsers.make[IO](sessionPool, c)
            o <- LiveOrders.make[IO](sessionPool)
            u1 <- u.create(uName, pass)
            o1 <- o.findBy(u1)
            o2 <- o.get(u1, oid)
            oid <- o.create(u1, pid, items, price)
          } yield {
            assert(
              o1.isEmpty && o2.isEmpty && oid.uuid.version === 4
            )
          }
        }
      }
    }

    test("Users") {
      forAll(MaxTests) { (username: UserName, password: Password) =>
        IOAssertion {
          for {
            c <- LiveCrypto.make[IO](salt)
            u <- LiveUsers.make[IO](sessionPool, c)
            uid <- u.create(username, password)
            u1 <- u.find(username, password)
            u2 <- u.find(username, "bad".coerce[Password]) // bad password
            x <- u.create(username, password).attempt // user already exists
          } yield {
            assert(u1.count(_.userId === uid) === 1 && u2.isEmpty && x.isLeft)
          }
        }
      }
    }

    test("Categories") {
      forAll(MaxTests) { (category: Category) =>
        for {
          c <- LiveCategories.make[IO](sessionPool)
          cs1 <- c.findAll
          _ <- c.create(category.name)
          cs2 <- c.findAll
          eCat <- c.create(category.name).attempt
        } yield {
          assert(cs1.isEmpty && cs2.count(_.name === category.name) === 1 && eCat.isLeft)
        }
      }
    }

    test("Brands") {
      forAll(MaxTests) { (brand: Brand) =>
        LiveBrands
          .make[IO](sessionPool)
        for {
          brandsAlg <- LiveBrands.make[IO](sessionPool)
          bs1 <- brandsAlg.findAll
          _ <- brandsAlg.create(brand.name)
          bs2 <- brandsAlg.findAll
          eBrand <- brandsAlg.create(brand.name).attempt
        } yield {
          assert(
            bs1.isEmpty &&
              bs2.count(_.name === brand.name) === 1 &&
              eBrand.isLeft
            )
        }
      }
    }

    test("Items") {
      forAll(MaxTests) { (item: Item) =>
        def newItem(b: Option[BrandId], c: Option[CategoryId]) =
          CreateItem(
            name = item.name,
            description = item.description,
            price = item.price,
            brandId = b.getOrElse(item.brand.uuid),
            categoryId = c.getOrElse(item.category.uuid)
            )

        IOAssertion {
          for {
            b <- LiveBrands.make[IO](sessionPool)
            c <- LiveCategories.make[IO](sessionPool)
            i <- LiveItems.make[IO](sessionPool)
            is1 <- i.findAll
            _ <- b.create(item.brand.name)
            bId <- b.findAll.map(_.headOption.map(_.uuid))
            _ <- c.create(item.category.name)
            cId <- c.findAll.map(_.headOption.map(_.uuid))
            _ <- i.create(newItem(bId, cId))
            is2 <- i.findAll
          } yield {
            assert(is1.isEmpty && is2.count(_.name === item.name) === 1)
          }
        }
      }
    }
  }
}
