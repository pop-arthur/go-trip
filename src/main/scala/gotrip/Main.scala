package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import gotrip.config.{DatabaseConfig, ServerConfig}
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.database.{Migration, SkunkSessionPool}
import gotrip.http.additionalservice.AdditionalServiceController
import gotrip.http.location.LocationController
import gotrip.http.provider.ProviderController
import gotrip.http.triplocation.TripLocationController
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.service.additionalservice.AdditionalServiceService
import gotrip.service.location.LocationService
import gotrip.service.provider.ProviderService
import gotrip.service.triplocation.TripLocationService
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
        val locationRepository = LocationRepository.makePostgres[IO](sessionPool)
        val locationService = LocationService[IO](locationRepository)
        val locationController = LocationController(locationService)

        val tripLocationRepository = TripLocationRepository.makePostgres[IO](sessionPool)
        val tripLocationService = TripLocationService[IO](tripLocationRepository)
        val tripLocationController = TripLocationController(tripLocationService)

        val providerRepository = ProviderRepository.makePostgres[IO](sessionPool)
        val providerService = ProviderService[IO](providerRepository)
        val providerController = ProviderController(providerService)

        val additionalServiceRepository = AdditionalServiceRepository.makePostgres[IO](sessionPool)
        val additionalServiceService = AdditionalServiceService[IO](additionalServiceRepository)
        val additionalServiceController = AdditionalServiceController(additionalServiceService)

        val serverEndpoints =
          locationController.all ++
            tripLocationController.all ++
            providerController.all ++
            additionalServiceController.all

        // Routes
        val swaggerEndpoints = SwaggerInterpreter()
          .fromServerEndpoints[IO](serverEndpoints, "GoTrip API", "0.1.0")
        val routes = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerEndpoints)
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
