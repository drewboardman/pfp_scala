package suite

import cats.effect.{ ContextShift, IO, Timer }
import org.scalatest.Assertion
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.UUID
import scala.concurrent.ExecutionContext

trait PureTestSuite extends AsyncFunSuite with ScalaCheckDrivenPropertyChecks with CatsEquality {
  implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  private def mkUnique(name: String): String = s"$name - ${UUID.randomUUID}"

  // this had an (implicit pos: Position) argument list as well
  def spec(testName: String)(f: => IO[Assertion]): Unit = test(mkUnique(testName))(IO.suspend(f).unsafeToFuture())
}
