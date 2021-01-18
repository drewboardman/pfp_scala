package shop.http.routes

import cats.effect.IO
import cats.implicits.none
import org.http4s.Method.GET
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Request, Status }
import shop.algebras.Items
import shop.arbitrary.{ arbBrand, arbItem }
import shop.domain.Brand.{ Brand, BrandName }
import shop.domain.Item.{ CreateItem, Item, ItemId, UpdateItem }
import shop.http.Json.itemEncoder
import suite.{ HttpTestSuite, IOAssertion }

class ItemRoutesSpec extends HttpTestSuite {
  def dataItems(items: List[Item]): TestItems =
    new TestItems {
      override def findAll: IO[List[Item]] = IO.pure(items)
    }

  test("GET items by brand [OK]") {
    forAll { (items: List[Item], brand: Brand) =>
      IOAssertion {
        val routes  = new ItemRoutes[IO](dataItems(items)).routes
        val request = Request[IO](GET, uri"/items".withQueryParam(brand.name.value))
        assertHttp(routes, request)(Status.Ok, items)
      }
    }
  }
}

protected class TestItems extends Items[IO] {
  override def create(item: CreateItem): IO[Unit]         = IO.unit
  override def findAll: IO[List[Item]]                    = IO.pure(List.empty)
  override def findBy(brand: BrandName): IO[List[Item]]   = IO.pure(List.empty)
  override def findById(itemId: ItemId): IO[Option[Item]] = IO.pure(none[Item])
  override def update(item: UpdateItem): IO[Unit]         = IO.unit
}
