package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.config.{AuthConfig, DatabaseConfig, ServerConfig}
import gotrip.database.{Migration, SkunkSessionPool}
import gotrip.http.auth.{AuthController, AuthSupport}
import gotrip.http.additionalservice.AdditionalServiceController
import gotrip.http.location.LocationController
import gotrip.http.provider.ProviderController
import gotrip.http.triplocation.TripLocationController
import gotrip.http.user.UserController
import gotrip.http.notification.NotificationController
import gotrip.http.notificationpreference.NotificationPreferenceController
import gotrip.http.achievement.{AchievementController, AdminAchievementController}
import gotrip.http.userachievement.UserAchievementController
import gotrip.http.review.ReviewController

import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.repository.user.UserRepository
import gotrip.repository.notification.NotificationRepository
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.achievement.AchievementRepository
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.repository.review.ReviewRepository
import gotrip.repository.auth.AuthSessionRepository

import gotrip.service.auth.{AuthService, JwtService, PasswordHasher}
import gotrip.service.additionalservice.AdditionalServiceService
import gotrip.service.location.LocationService
import gotrip.service.provider.ProviderService
import gotrip.service.triplocation.TripLocationService
import gotrip.service.user.UserService
import gotrip.service.notification.NotificationService
import gotrip.service.notificationpreference.NotificationPreferenceService
import gotrip.service.achievement.AchievementService
import gotrip.service.userachievement.UserAchievementService
import gotrip.service.review.ReviewService

import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
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
      authConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.auth").load[AuthConfig]
          .leftMap(e => ConfigReaderException[AuthConfig](e))
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
        val notificationRepository = NotificationRepository.makePostgres[IO](sessionPool)
        val notifPrefRepository = NotificationPreferenceRepository.makePostgres[IO](sessionPool)
        val achievementRepository = AchievementRepository.makePostgres[IO](sessionPool)
        val userAchievementRepository = UserAchievementRepository.makePostgres[IO](sessionPool)
        val reviewRepository = ReviewRepository.makePostgres[IO](sessionPool)
        val authSessionRepository = AuthSessionRepository.makePostgres[IO](sessionPool)

        // ---- Service Layer ----
        val jwtService = new JwtService[IO](authConfig)
        val passwordHasher = PasswordHasher.bcrypt[IO](authConfig.passwordCost)
        val locationService = LocationService[IO](locationRepository)
        val tripLocationService = TripLocationService[IO](tripLocationRepository)
        val providerService = ProviderService[IO](providerRepository)
        val additionalServiceService = AdditionalServiceService[IO](additionalServiceRepository)
        val userService = new UserService[IO](userRepository)
        val notificationService = new NotificationService[IO](notificationRepository)
        val notifPrefService = new NotificationPreferenceService[IO](notifPrefRepository)
        val achievementService = new AchievementService[IO](achievementRepository)
        val userAchievementService = new UserAchievementService[IO](userAchievementRepository)
        val reviewService = new ReviewService[IO](reviewRepository)
        val authService = new AuthService[IO](
          userRepository,
          authSessionRepository,
          passwordHasher,
          jwtService,
          authConfig.refreshTokenTtl
        )

        // ---- Controller Layer ----
        val authSupport = new AuthSupport(jwtService)
        val authController = new AuthController(authService, authSupport)
        val locationController = new LocationController(locationService, authSupport)
        val tripLocationController = new TripLocationController(tripLocationService, authSupport)
        val providerController = new ProviderController(providerService, authSupport)
        val additionalServiceController = new AdditionalServiceController(additionalServiceService, authSupport)
        val userController = new UserController(userService, authSupport)
        val notificationController = new NotificationController(notificationService, authSupport)
        val notifPrefController = new NotificationPreferenceController(notifPrefService, authSupport)
        val achievementController = new AchievementController(achievementService, authSupport)
        val adminAchievementController = new AdminAchievementController(achievementService, authSupport)
        val userAchievementController = new UserAchievementController(userAchievementService, authSupport)
        val reviewController = new ReviewController(reviewService, authSupport)

        // ---- Сборка всех эндпоинтов ----
        val serverEndpoints =
          authController.all ++
          locationController.all ++
          tripLocationController.all ++
          providerController.all ++
          additionalServiceController.all ++
          userController.all ++
          notificationController.all ++
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

        // ---- CORS middleware (разрешить все источники для разработки) ----
        val corsApp = CORS.policy
          .withAllowOriginAll
          .apply(httpApp)

        IO.println(s"Starting HTTP server on ${serverConfig.host}:${serverConfig.port}...") >>
          BlazeServerBuilder[IO]
            .withExecutionContext(ExecutionContext.global)
            .bindHttp(serverConfig.port, serverConfig.host)
            .withHttpApp(corsApp)
            .resource
            .use(_ => IO.never)
      }
    } yield ()
}