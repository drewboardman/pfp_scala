package shop.http.routes

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import shop.algebras.Brands
import shop.domain.Brand.{ Brand, BrandName }
import shop.http.Json._
import suite.{ IOAssertion, PureTestSuite }
import shop.arbitrary._

class BrandRoutesSpec extends PureTestSuite {
  def dataBrands(brands: List[Brand]): TestBrands =
    new TestBrands {
      override def findAll: IO[List[Brand]] = IO.pure(brands)
    }

  test("GET brands [OK]") {
    forAll { (bs: List[Brand]) =>
      IOAssertion {
        Request[IO](GET, uri"/brands").pure[IO].flatMap { request =>
          new BrandRoutes[IO](dataBrands(bs)).routes
            .run(request)
            .value
            .flatMap {
              case Some(resp) =>
                resp.asJson.map { json =>
                  assert(
                    resp.status === Status.Ok &&
                    json.dropNullValues === bs.asJson.dropNullValues
                  )
                }
              case None       =>
                fail("route not found")
            }
        }
      }
    }
  }
}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[Unit] = IO.unit
  def findAll: IO[List[Brand]]          = IO.pure(List.empty)
}
