package shop

import cats.effect._
import cats.effect.concurrent.Ref
import com.olegpy.meow.effects._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder
import shop.config.Data.AppConfig
import shop.modules._

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val configLoader: IO[Ref[IO, AppConfig]] = config.loader[IO].flatMap(Ref.of[IO, AppConfig])

  override def run(args: List[String]): IO[ExitCode] =
    configLoader.flatMap { refAppConfig =>
      refAppConfig.runAsk { implicit askAppConfig =>
        val ex = ExecutionContext.global
        AppResources.loadResources[IO](ex) { cfg => resources =>
          for {
            sec <- Security.make[IO](cfg, resources.psql, resources.redis)
            alg <- Algebras.make[IO](resources.redis, resources.psql, cfg.cartExpiration)
            clients <- HttpClients.make[IO](cfg.paymentConfig, resources.client)
            programs <- Programs.make[IO](cfg.checkoutConfig, alg, clients)
            api <- HttpApi.make[IO](alg, programs, sec)
            _ <- BlazeServerBuilder[IO](ex)
                   .bindHttp(cfg.httpServerConfig.port.value, cfg.httpServerConfig.host.value)
                   .withHttpApp(api.httpApp)
                   .serve
                   .compile
                   .drain
          } yield ExitCode.Success
        }
      }
    }
}
