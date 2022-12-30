package com.cargo

import com.cargo.algebra.{Authentication, CarOffers, Tokens}
import com.cargo.api.MainController
import com.cargo.api.generated.Resource
import com.cargo.config.{ApplicationConfig, SendGridConfig}
import com.cargo.infrastructure.DatabaseTransactor
import com.cargo.repository.{CarOffersRepository, UsersRepository}
import com.cargo.service.EmailNotification
import com.sendgrid.SendGrid
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.ZIOAppDefault
import zio._
import zio.interop.catz._
import org.http4s.server.middleware.CORS
import org.http4s.implicits._
import zio.logging.LogFormat
import zio.config.syntax._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    app.provide(
      SLF4J.slf4j(LogFormat.colored),
      DatabaseTransactor.live,
      ApplicationConfig.live,
      ApplicationConfig.live.narrow(_.token),
      ApplicationConfig.live.narrow(_.database),
      ApplicationConfig.live.narrow(_.sendgridConfig),
      Authentication.live,
      UsersRepository.live,
      Tokens.live,
      ZLayer(ZIO.service[SendGridConfig].map(cfg => new SendGrid(cfg.sendGridApiKey))),
      EmailNotification.live,
      CarOffers.live,
      CarOffersRepository.live
    )

  lazy val app =
    ZIO.service[ApplicationConfig].flatMap { cfg =>
      ZIO.runtime
        .flatMap { implicit r: Runtime[Any] =>
          val cors = CORS.policy
            .withAllowCredentials(true)
            .withAllowOriginHostCi(cfg.server.allowedOrigins.contains)

          println(s"CORS config: " + cfg.server.allowedOrigins)

          val api =
            Router(
              "/" -> new Resource[RIO[Authentication with CarOffers, *]]().routes(new MainController)
            ).orNotFound

          BlazeServerBuilder[RIO[Authentication with CarOffers, *]]
            .withHttpApp(cors(api))
            .bindHttp(cfg.server.port, cfg.server.hostname)
            .serve
            .compile
            .drain
        }
    }
}
