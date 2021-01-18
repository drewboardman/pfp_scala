package suite

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.{ HttpRoutes, Request, Status }
import org.scalatest.Assertion

import scala.util.control.NoStackTrace

trait HttpTestSuite extends PureTestSuite {
  case object DummyError extends NoStackTrace

  def assertHttp[A: Encoder](routes: HttpRoutes[IO], request: Request[IO])(
      expectedStatus: Status,
      expectedBody: A
  ): IO[Assertion] =
    routes.run(request).value.flatMap {
      case Some(response) =>
        response.asJson.map { json =>
          assert(
            response.status === expectedStatus &&
            json.dropNullValues === expectedBody.asJson.dropNullValues
          )
        }
      case None           => fail("route not found")
    }

  def assertHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status): IO[Assertion] =
    routes.run(req).value.map {
      case Some(resp) =>
        assert(resp.status === expectedStatus)
      case None       => fail("route not found")
    }

  def assertHttpFailure(routes: HttpRoutes[IO], req: Request[IO]): IO[Assertion] =
    routes.run(req).value.attempt.map {
      case Left(_)  => assert(true)
      case Right(_) => fail("expected a failure")
    }
}
