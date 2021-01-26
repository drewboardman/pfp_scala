package suite

import cats.effect.concurrent.Deferred
import cats.effect.{ IO, Resource }
import org.scalatest.BeforeAndAfterAll

trait ResourceSuite[A] extends PureTestSuite with BeforeAndAfterAll {
  def resources: Resource[IO, A]

  private[this] var res: A            = _
  private[this] var cleanUp: IO[Unit] = _
  private[this] val latch             = Deferred[IO, Unit].unsafeRunSync()

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (r, h) = resources.allocated.unsafeRunSync()
    res = r
    cleanUp = h
    latch.complete(()).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    cleanUp.unsafeRunSync()
    super.afterAll()
  }

  def withResources(f: (=> A) => Unit): Unit =
    f {
      latch.get.unsafeRunSync()
      res
    }
}
