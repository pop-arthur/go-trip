package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import gotrip.config.{DatabaseConfig, ServerConfig}
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.database.{Migration, SkunkSessionPool}
import gotrip.http.location.LocationController
import gotrip.repository.location.LocationRepository
import gotrip.service.location.LocationService
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      // Configuration
      databaseConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.database").load[DatabaseConfig]
          .leftMap(e => ConfigReaderException[DatabaseConfig](e))
      )
      serverConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.server").load[ServerConfig]
          .leftMap(e => ConfigReaderException[ServerConfig](e))
      )

      // Migration
      _ <- IO.println("Running Flyway migrations...")
      _ <- Migration.migrate[IO](databaseConfig)
      
      // Session
      _ <- IO.println("Initializing Skunk session pool...")
      _ <- SkunkSessionPool[IO](databaseConfig).use { sessionPool =>
        // Layers
        val repository = LocationRepository.makePostgres[IO](sessionPool)
        val service = LocationService[IO](repository)
        val controller = LocationController(service)
        // Routes
        val swaggerEndpoints = SwaggerInterpreter()
          .fromServerEndpoints[IO](controller.all, "GoTrip API", "0.1.0")
        val routes = Http4sServerInterpreter[IO]().toRoutes(controller.all ++ swaggerEndpoints)
        val httpApp = Router("/" -> routes).orNotFound

        IO.println(s"Starting HTTP server on ${serverConfig.host}:${serverConfig.port}...") >>
          // Server
          BlazeServerBuilder[IO]
            .withExecutionContext(ExecutionContext.global)
            .bindHttp(serverConfig.port, serverConfig.host)
            .withHttpApp(httpApp)
            .resource
            .use(_ => IO.never)
      }
    } yield ()
}
