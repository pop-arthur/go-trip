package gotrip.service.achievement

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement._
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.order.{Order, OrderId, OrderStatus, OrderTitle}
import gotrip.domain.review._
import gotrip.domain.trip._
import gotrip.domain.user.UserId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.achievement.AchievementRepository
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.review.ReviewRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.triplocation.TripLocationRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, LocalDate}
import java.util.UUID

class AchievementEngineSpec extends AnyWordSpec with Matchers with MockFactory {

  "AchievementEngine" should {
    "unlock achievement when condition is met (TripsCount)" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.TripsCount, 1)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      tripRepo.countByUser.expects(userId).returning(IO.pure(1))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(
        UserAchievement(UserAchievementId(UUID.randomUUID()), userId, achievement.id, Instant.now(), Instant.now(), Instant.now())
      )))

      engine.checkAndUnlock(userId, AchievementEvent.TripCreated(mockTrip(userId))).unsafeRunSync()
    }

    "not unlock achievement when condition is not met" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.TripsCount, 5)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      tripRepo.countByUser.expects(userId).returning(IO.pure(2))

      userAchievementRepo.create.expects(*).never()

      engine.checkAndUnlock(userId, AchievementEvent.TripCreated(mockTrip(userId))).unsafeRunSync()
    }

    "unlock achievement for OrdersCount" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.OrdersCount, 1)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      orderRepo.countByUser.expects(userId).returning(IO.pure(1))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, achievement.id))))

      engine.checkAndUnlock(userId, AchievementEvent.OrderCreated(mockOrder(userId))).unsafeRunSync()
    }

    "unlock achievement for ReviewsCount" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.ReviewsCount, 1)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      reviewRepo.countByUser.expects(userId).returning(IO.pure(1))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, achievement.id))))

      engine.checkAndUnlock(userId, AchievementEvent.ReviewCreated(mockReview(userId))).unsafeRunSync()
    }

    "unlock achievement for SpendingAmount" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.SpendingAmount, 1000)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      orderRepo.getTotalSpending.expects(userId).returning(IO.pure(1500.0))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, achievement.id))))

      engine.checkAndUnlock(userId, AchievementEvent.OrderCreated(mockOrder(userId))).unsafeRunSync()
    }

    "unlock achievement for CountriesCount" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.CountriesCount, 2)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))
      tripLocationRepo.countDistinctCountries.expects(userId).returning(IO.pure(2))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, achievement.id))))

      engine.checkAndUnlock(userId, AchievementEvent.TripCreated(mockTrip(userId))).unsafeRunSync()
    }

    "not unlock already unlocked achievements" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val achievement = createAchievement(AchievementConditionType.TripsCount, 1)
      val existing = mockUserAchievement(userId, achievement.id)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(achievement)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(List(existing)))

      tripRepo.countByUser.expects(*).never()
      userAchievementRepo.create.expects(*).never()

      engine.checkAndUnlock(userId, AchievementEvent.TripCreated(mockTrip(userId))).unsafeRunSync()
    }

    "handle multiple achievements and unlock all that are met" in {
      val (engine, repos) = createEngine()
      import repos._

      val userId = UserId(UUID.randomUUID())
      val ach1 = createAchievement(AchievementConditionType.TripsCount, 1)
      val ach2 = createAchievement(AchievementConditionType.OrdersCount, 2)
      val ach3 = createAchievement(AchievementConditionType.ReviewsCount, 10)

      (() => achievementRepo.findAll()).expects().returning(IO.pure(List(ach1, ach2, ach3)))
      userAchievementRepo.findByUserId.expects(userId).returning(IO.pure(Nil))

      tripRepo.countByUser.expects(userId).returning(IO.pure(1))
      orderRepo.countByUser.expects(userId).returning(IO.pure(2))
      reviewRepo.countByUser.expects(userId).returning(IO.pure(3))

      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, ach1.id)))).once()
      userAchievementRepo.create.expects(*).returning(IO.pure(Some(mockUserAchievement(userId, ach2.id)))).once()

      engine.checkAndUnlock(userId, AchievementEvent.TripCreated(mockTrip(userId))).unsafeRunSync()
    }
  }

  private def createEngine(): (AchievementEngine[IO], TestRepos) = {
    val repos = TestRepos(
      achievementRepo = mock[AchievementRepository[IO]],
      userAchievementRepo = mock[UserAchievementRepository[IO]],
      tripRepo = mock[TripRepository[IO]],
      orderRepo = mock[OrderRepository[IO]],
      reviewRepo = mock[ReviewRepository[IO]],
      tripLocationRepo = mock[TripLocationRepository[IO]]
    )
    val engine = new AchievementEngine[IO](
      repos.achievementRepo,
      repos.userAchievementRepo,
      repos.tripRepo,
      repos.orderRepo,
      repos.reviewRepo,
      repos.tripLocationRepo
    )
    (engine, repos)
  }

  private case class TestRepos(
    achievementRepo: AchievementRepository[IO],
    userAchievementRepo: UserAchievementRepository[IO],
    tripRepo: TripRepository[IO],
    orderRepo: OrderRepository[IO],
    reviewRepo: ReviewRepository[IO],
    tripLocationRepo: TripLocationRepository[IO]
  )

  private def createAchievement(conditionType: AchievementConditionType, value: Int): Achievement =
    Achievement(
      id = AchievementId(UUID.randomUUID()),
      code = AchievementCode(s"TEST_${conditionType}_$value"),
      title = AchievementTitle("Test"),
      description = AchievementDescription(Some("Test")),
      conditionType = conditionType,
      conditionValue = value,
      iconUrl = AchievementIconUrl(None),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  private def mockTrip(userId: UserId): Trip =
    Trip(
      id = TripId(UUID.randomUUID()),
      user_id = userId,
      title = TripTitle("Test"),
      start_date = TripStartDate(None),
      end_date = TripEndDate(None),
      status = TripStatus.Planned,
      created_at = Instant.now(),
      updated_at = Instant.now()
    )

  private def mockOrder(userId: UserId): Order =
    Order(
      id = OrderId(UUID.randomUUID()),
      user_id = userId,
      trip_id = TripId(UUID.randomUUID()),
      provider_id = None,
      service_type = ServiceType.Other,
      external_order_id = None,
      title = OrderTitle("Test"),
      status = OrderStatus.PendingVerification,
      price_amount = Some(100.0),
      price_currency = Some("USD"),
      start_datetime = None,
      end_datetime = None,
      departure_location_id = None,
      arrival_location_id = None,
      created_at = Instant.now(),
      updated_at = Instant.now()
    )

  private def mockReview(userId: UserId): Review =
    Review(
      id = ReviewId(UUID.randomUUID()),
      userId = userId,
      targetType = ReviewTargetType.Provider,
      targetId = ReviewTargetId(UUID.randomUUID()),
      rating = ReviewRating(5),
      text = ReviewText(None),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  private def mockUserAchievement(userId: UserId, achievementId: AchievementId): UserAchievement =
    UserAchievement(
      id = UserAchievementId(UUID.randomUUID()),
      userId = userId,
      achievementId = achievementId,
      unlockedAt = Instant.now(),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
}