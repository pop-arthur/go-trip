package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.config.{DatabaseConfig, ServerConfig}
import gotrip.database.{Migration, SkunkSessionPool}
import gotrip.http.additionalservice.AdditionalServiceController
import gotrip.http.location.LocationController
import gotrip.http.provider.ProviderController
import gotrip.http.triplocation.TripLocationController
import gotrip.http.user.UserController
import gotrip.http.notificationpreference.NotificationPreferenceController
import gotrip.http.achievement.{AchievementController, AdminAchievementController}
import gotrip.http.userachievement.UserAchievementController
import gotrip.http.review.ReviewController

import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.repository.user.UserRepository
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.achievement.AchievementRepository
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.repository.review.ReviewRepository

import gotrip.service.additionalservice.AdditionalServiceService
import gotrip.service.location.LocationService
import gotrip.service.provider.ProviderService
import gotrip.service.triplocation.TripLocationService
import gotrip.service.user.UserService
import gotrip.service.notificationpreference.NotificationPreferenceService
import gotrip.service.achievement.AchievementService
import gotrip.service.userachievement.UserAchievementService
import gotrip.service.review.ReviewService

import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      databaseConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.database").load[DatabaseConfig]
          .leftMap(e => ConfigReaderException[DatabaseConfig](e))
      )
      serverConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.server").load[ServerConfig]
          .leftMap(e => ConfigReaderException[ServerConfig](e))
      )

      _ <- IO.println("Running Flyway migrations...")
      _ <- Migration.migrate[IO](databaseConfig)

      _ <- IO.println("Initializing Skunk session pool...")
      _ <- SkunkSessionPool[IO](databaseConfig).use { sessionPool =>
        // ---- Repository Layer ----
        val locationRepository = LocationRepository.makePostgres[IO](sessionPool)
        val tripLocationRepository = TripLocationRepository.makePostgres[IO](sessionPool)
        val providerRepository = ProviderRepository.makePostgres[IO](sessionPool)
        val additionalServiceRepository = AdditionalServiceRepository.makePostgres[IO](sessionPool)
        val userRepository = UserRepository.makePostgres[IO](sessionPool)
        val notifPrefRepository = NotificationPreferenceRepository.makePostgres[IO](sessionPool)
        val achievementRepository = AchievementRepository.makePostgres[IO](sessionPool)
        val userAchievementRepository = UserAchievementRepository.makePostgres[IO](sessionPool)
        val reviewRepository = ReviewRepository.makePostgres[IO](sessionPool)

        // ---- Service Layer ----
        val locationService = LocationService[IO](locationRepository)
        val tripLocationService = TripLocationService[IO](tripLocationRepository)
        val providerService = ProviderService[IO](providerRepository)
        val additionalServiceService = AdditionalServiceService[IO](additionalServiceRepository)
        val userService = new UserService[IO](userRepository)
        val notifPrefService = new NotificationPreferenceService[IO](notifPrefRepository)
        val achievementService = new AchievementService[IO](achievementRepository)
        val userAchievementService = new UserAchievementService[IO](userAchievementRepository)
        val reviewService = new ReviewService[IO](reviewRepository)

        // ---- Controller Layer ----
        val locationController = LocationController(locationService)
        val tripLocationController = TripLocationController(tripLocationService)
        val providerController = ProviderController(providerService)
        val additionalServiceController = AdditionalServiceController(additionalServiceService)
        val userController = new UserController(userService)
        val notifPrefController = new NotificationPreferenceController(notifPrefService)
        val achievementController = new AchievementController(achievementService)
        val adminAchievementController = new AdminAchievementController(achievementService)
        val userAchievementController = new UserAchievementController(userAchievementService)
        val reviewController = new ReviewController(reviewService)

        // ---- Сборка всех эндпоинтов ----
        val serverEndpoints =
          locationController.all ++
          tripLocationController.all ++
          providerController.all ++
          additionalServiceController.all ++
          userController.all ++
          notifPrefController.all ++
          achievementController.all ++
          adminAchievementController.all ++
          userAchievementController.all ++
          reviewController.all

        // ---- Swagger ----
        val swaggerEndpoints = SwaggerInterpreter()
          .fromServerEndpoints[IO](serverEndpoints, "GoTrip API", "0.1.0")
        val routes = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerEndpoints)
        val httpApp = Router("/" -> routes).orNotFound

        IO.println(s"Starting HTTP server on ${serverConfig.host}:${serverConfig.port}...") >>
          BlazeServerBuilder[IO]
            .withExecutionContext(ExecutionContext.global)
            .bindHttp(serverConfig.port, serverConfig.host)
            .withHttpApp(httpApp)
            .resource
            .use(_ => IO.never)
      }
    } yield ()
}