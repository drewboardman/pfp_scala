package shop.algebras

import cats.Eq
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits.{catsSyntaxEq => _, _}
import ciris.Secret
import eu.timepit.refined.auto._
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken, jwtDecode}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.cats.refTypeShow
import eu.timepit.refined.types.string.NonEmptyString
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import shop.arbitrary.{arbCoercibleInt, arbCoercibleStr, arbCoercibleUUID, arbItem}
import shop.config.Data.{JwtSecretKeyConfig, ShoppingCartExpiration, TokenExpiration}
import shop.domain.Auth._
import shop.domain.Brand._
import shop.domain.Category._
import shop.domain.Item.{CreateItem, Item, ItemId, UpdateItem}
import shop.domain.ShoppingCart.{Cart, Quantity}
import shop.logger.NoOp
import shop.effects.GenUUID
import shop.http.auth.Users.{User, UserJwtAuth}
import suite.{IOAssertion, ResourceSuite}

import java.util.UUID
import scala.concurrent.duration.DurationInt

class RedisTest extends ResourceSuite[RedisCommands[IO, String, String]] {
  val MaxTests: PropertyCheckConfigParam = MinSuccessful(1)

  override def resources: Resource[IO, RedisCommands[IO, String, String]] =
    Redis[IO].utf8("redis://localhost")
  lazy val Exp         = ShoppingCartExpiration(30.seconds)
  lazy val tokenConfig = JwtSecretKeyConfig(Secret("bar": NonEmptyString))
  lazy val tokenExp    = TokenExpiration(30.seconds)
  lazy val jwtClaim    = JwtClaim("test")
  lazy val userJwtAuth = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  withResources { cmd =>
    test("Auth") {
      forAll(MaxTests) { (un1: UserName, un2: UserName, pass: Password) =>
        IOAssertion {
          for {
            t <- LiveTokens.make[IO](tokenConfig, tokenExp)
            a <- LiveAuth.make(tokenExp, t, new TestUsers(un2), cmd)
            u <- LiveUsersAuth.make[IO](cmd)
            x <- u.findUser(JwtToken("invalid"))(jwtClaim)
            j <- a.newUser(un1, pass)
            e <- jwtDecode[IO](j, userJwtAuth.value).attempt
            k <- a.login(un2, pass)
            f <- jwtDecode[IO](k, userJwtAuth.value).attempt
            _ <- a.logout(k, un2)
            y <- u.findUser(k)(jwtClaim)
            w <- u.findUser(j)(jwtClaim)
          } yield assert(
            x.isEmpty && e.isRight && f.isRight && y.isEmpty &&
              w.fold(false)(_.user.userName === un1)
            )
        }
      }
    }

    test("Shopping Cart") {
      forAll(MaxTests) { (uid: UserId, it1: Item, it2: Item, q1: Quantity, q2: Quantity) =>
        IOAssertion {
          val init = Map(
            (it1.uuid -> it1),
            (it2.uuid -> it2)
            )
          for {
            ref <- Ref.of[IO, Map[ItemId, Item]](init)
            items = new TestItems(ref)
            cart <- LiveShoppingCart.make[IO](items, cmd, Exp)
            total1 <- cart.get(uid)
            _ <- cart.add(uid, it1.uuid, q1)
            _ <- cart.add(uid, it2.uuid, q1)
            total2 <- cart.get(uid)
            _ <- cart.removeItem(uid, it1.uuid)
            total3 <- cart.get(uid)
            _ <- cart.update(uid, Cart(Map(it2.uuid -> q2)))
            total4 <- cart.get(uid)
            _ <- cart.delete(uid)
            total5 <- cart.get(uid)
          } yield assert(
            total1.items.isEmpty &&
              total2.items.size === 2 &&
              total3.items.size === 1 &&
              total4.items.headOption.fold(false)(_.quantity === q2) &&
              total5.items.isEmpty
            )
        }
      }
    }
  }
}

protected class TestUsers(un: UserName) extends Users[IO] {
  def find(username: UserName, password: Password): IO[Option[User]] =
    Eq[UserName]
      .eqv(username, un)
      .guard[Option]
      .as(User(UserId(UUID.randomUUID), un))
      .pure[IO]
  def create(username: UserName, password: Password): IO[UserId] =
    GenUUID[IO].make[UserId]
}

// using Ref for in-memory implementation
protected class TestItems(ref: Ref[IO, Map[ItemId, Item]]) extends Items[IO] {
  def findAll: IO[List[Item]] =
    ref.get.map(_.values.toList)
  def findBy(brand: BrandName): IO[List[Item]] =
    ref.get.map(_.values.filter(_.brand.name == brand).toList)
  def findById(itemId: ItemId): IO[Option[Item]] =
    ref.get.map(_.get(itemId))
  def create(item: CreateItem): IO[Unit] =
    GenUUID[IO].make[ItemId].flatMap { id =>
      val brand    = Brand(item.brandId, BrandName("foo"))
      val category = Category(item.categoryId, CategoryName("foo"))
      val newItem  = Item(id, item.name, item.description, item.price, brand, category)
      ref.update(_.updated(id, newItem))
    }
  def update(item: UpdateItem): IO[Unit] =
    ref
      .update { x =>
        x
          .get(item.id)
          .fold(x) { i =>
            x.updated(item.id, i.copy(price = item.price))
          }
      }
}
