package gotrip.service.achievement

import cats.Monad
import cats.syntax.all._
import gotrip.domain.achievement.{Achievement, AchievementConditionType}
import gotrip.domain.order.Order
import gotrip.domain.review.Review
import gotrip.domain.trip.Trip
import gotrip.domain.user.UserId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.achievement.AchievementRepository
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.review.ReviewRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.triplocation.TripLocationRepository

import java.time.Instant
import java.util.UUID

class AchievementEngine[F[_]: Monad](
  achievementRepo: AchievementRepository[F],
  userAchievementRepo: UserAchievementRepository[F],
  tripRepo: TripRepository[F],
  orderRepo: OrderRepository[F],
  reviewRepo: ReviewRepository[F],
  tripLocationRepo: TripLocationRepository[F]
) {

  def checkAndUnlock(userId: UserId, event: AchievementEvent): F[Unit] =
    for {
      all <- achievementRepo.findAll()
      userAchievements <- userAchievementRepo.findByUserId(userId)
      unlockedIds = userAchievements.map(_.achievementId).toSet
      pending = all.filterNot(a => unlockedIds(a.id))
      _ <- pending.traverse_ { ach =>
        checkCondition(userId, ach).flatMap {
          case true =>
            val ua = UserAchievement(
              id = UserAchievementId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
              userId = userId,
              achievementId = ach.id,
              unlockedAt = Instant.now(),
              createdAt = Instant.now(),
              updatedAt = Instant.now()
            )
            userAchievementRepo.create(ua).void
          case false => Monad[F].unit
        }
      }
    } yield ()

  private def checkCondition(userId: UserId, achievement: Achievement): F[Boolean] =
    achievement.conditionType match {
      case AchievementConditionType.TripsCount =>
        tripRepo.countByUser(userId).map(_ >= achievement.conditionValue)
      case AchievementConditionType.CountriesCount =>
        tripLocationRepo.countDistinctCountries(userId).map(_ >= achievement.conditionValue)
      case AchievementConditionType.OrdersCount =>
        orderRepo.countByUser(userId).map(_ >= achievement.conditionValue)
      case AchievementConditionType.ReviewsCount =>
        reviewRepo.countByUser(userId).map(_ >= achievement.conditionValue)
      case AchievementConditionType.SpendingAmount =>
        orderRepo.getTotalSpending(userId).map(_ >= achievement.conditionValue.toDouble)
    }
}

sealed trait AchievementEvent
object AchievementEvent {
  case class TripCreated(trip: Trip) extends AchievementEvent
  case class TripCompleted(trip: Trip) extends AchievementEvent
  case class OrderCreated(order: Order) extends AchievementEvent
  case class ReviewCreated(review: Review) extends AchievementEvent
}