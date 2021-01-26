package shop.http.routes

import cats.effect.IO
import org.http4s.Method._
import org.http4s._
import org.http4s.implicits._
import shop.algebras.Brands
import shop.arbitrary._
import shop.domain.Brand.{ Brand, BrandName }
import shop.http.Json._
import suite.{ HttpTestSuite, IOAssertion }

class BrandRoutesSpec extends HttpTestSuite {
  def dataBrands(brands: List[Brand]): TestBrands =
    new TestBrands {
      override def findAll: IO[List[Brand]] = IO.pure(brands)
    }

  def failBrands(brands: List[Brand]): TestBrands =
    new TestBrands {
      override def findAll: IO[List[Brand]] = IO.raiseError(DummyError) *> IO.pure(brands)
    }

  test("GET brands [OK]") {
    forAll { (bs: List[Brand]) =>
      IOAssertion {
        val routes  = new BrandRoutes[IO](dataBrands(bs)).routes
        val request = Request[IO](GET, uri"/brands")
        assertHttp(routes, request)(Status.Ok, bs)
      }
    }
  }

  test("GET brands [ERROR]") {
    forAll { (brands: List[Brand]) =>
      IOAssertion {
        val routes  = new BrandRoutes[IO](failBrands(brands)).routes
        val request = Request[IO](GET, uri"/brands")
        assertHttpFailure(routes, request)
      }
    }
  }
}

protected class TestBrands extends Brands[IO] {
  def create(name: BrandName): IO[Unit] = IO.unit
  def findAll: IO[List[Brand]]          = IO.pure(List.empty)
}
