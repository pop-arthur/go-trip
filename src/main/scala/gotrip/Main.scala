package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.config.{AuthConfig, DatabaseConfig, ServerConfig}
import gotrip.database.{Migration, SkunkSessionPool}
import gotrip.integration.duffel.{DuffelConfig, DuffelOrderStatusProvider}
import gotrip.http.auth.{AuthController, AuthSupport}
import gotrip.http.additionalservice.AdditionalServiceController
import gotrip.http.location.LocationController
import gotrip.http.provider.ProviderController
import gotrip.http.trip.TripController
import gotrip.http.triplocation.TripLocationController
import gotrip.http.order.OrderController
import gotrip.http.orderfile.OrderFileController
import gotrip.http.user.UserController
import gotrip.http.notification.NotificationController
import gotrip.http.notificationpreference.NotificationPreferenceController
import gotrip.http.achievement.{AchievementController, AdminAchievementController}
import gotrip.http.userachievement.UserAchievementController
import gotrip.http.review.ReviewController
import gotrip.http.recommendation.RecommendationController

import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.orderfile.OrderFileRepository
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
import gotrip.service.trip.TripService
import gotrip.service.triplocation.TripLocationService
import gotrip.service.order.{OrderService, OrderStatusSyncService}
import gotrip.service.orderfile.OrderFileService
import gotrip.service.user.UserService
import gotrip.service.notification.NotificationService
import gotrip.service.notificationpreference.NotificationPreferenceService
import gotrip.service.achievement.AchievementService
import gotrip.service.userachievement.UserAchievementService
import gotrip.service.review.ReviewService
import gotrip.service.recommendation.RecommendationService

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
      duffelConfig <- IO.fromEither(
        ConfigSource.default.at("gotrip.duffel").load[DuffelConfig]
          .leftMap(e => ConfigReaderException[DuffelConfig](e))
      )

      _ <- IO.println("Running Flyway migrations...")
      _ <- Migration.migrate[IO](databaseConfig)

      _ <- IO.println("Initializing Skunk session pool...")
      _ <- SkunkSessionPool[IO](databaseConfig).use { sessionPool =>
        // ---- Repository Layer ----
        val locationRepository = LocationRepository.makePostgres[IO](sessionPool)
        val tripRepository = TripRepository.makePostgres[IO](sessionPool)
        val tripLocationRepository = TripLocationRepository.makePostgres[IO](sessionPool)
        val orderRepository = OrderRepository.makePostgres[IO](sessionPool)
        val orderFileRepository = OrderFileRepository.makePostgres[IO](sessionPool)
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
        val tripService = TripService[IO](tripRepository)
        val tripLocationService = TripLocationService[IO](tripLocationRepository)
        val providerService = ProviderService[IO](providerRepository)
        val additionalServiceService = AdditionalServiceService[IO](additionalServiceRepository)
        val userService = new UserService[IO](userRepository)
        val notificationService = new NotificationService[IO](notificationRepository)
        val orderStatusProvider = DuffelOrderStatusProvider.make[IO](duffelConfig)
        val notifPrefService = new NotificationPreferenceService[IO](notifPrefRepository)
        val orderService = new OrderService[IO](orderRepository, notifPrefRepository, notificationService)
        val orderStatusSyncService = new OrderStatusSyncService[IO](
          orderRepository,
          orderService,
          orderStatusProvider
        )
        val orderFileService = new OrderFileService[IO](orderFileRepository)
        val achievementService = new AchievementService[IO](achievementRepository)
        val userAchievementService = new UserAchievementService[IO](userAchievementRepository)
        val reviewService = new ReviewService[IO](reviewRepository)
        val recommendationService = new RecommendationService[IO](
          orderRepository,
          tripLocationRepository,
          additionalServiceRepository
        )
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
        val tripController = new TripController(tripService, authSupport)
        val tripLocationController = new TripLocationController(tripLocationService, authSupport)
        val orderController = new OrderController(orderService, authSupport)
        val orderFileController = new OrderFileController(orderFileService, authSupport)
        val providerController = new ProviderController(providerService, authSupport)
        val additionalServiceController = new AdditionalServiceController(additionalServiceService, authSupport)
        val userController = new UserController(userService, authSupport)
        val notificationController = new NotificationController(
          notificationService,
          authSupport,
          orderStatusSyncService
        )
        val notifPrefController = new NotificationPreferenceController(notifPrefService, authSupport)
        val achievementController = new AchievementController(achievementService, authSupport)
        val adminAchievementController = new AdminAchievementController(achievementService, authSupport)
        val userAchievementController = new UserAchievementController(userAchievementService, authSupport)
        val reviewController = new ReviewController(reviewService, authSupport)
        val recommendationController = new RecommendationController(recommendationService, authSupport)

        // ---- Сборка всех эндпоинтов ----
        val serverEndpoints =
          authController.all ++
          locationController.all ++
          tripController.all ++
          tripLocationController.all ++
          orderController.all ++
          orderFileController.all ++
          providerController.all ++
          additionalServiceController.all ++
          userController.all ++
          notificationController.all ++
          notifPrefController.all ++
          achievementController.all ++
          adminAchievementController.all ++
          userAchievementController.all ++
          reviewController.all ++
          recommendationController.all

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
