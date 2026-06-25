package gotrip.repository.userachievement

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import java.time.Instant
import scala.collection.mutable

object InMemoryUserAchievementRepository {
  def make[F[_]: Applicative]: F[UserAchievementRepository[F]] = {
    val store = mutable.ListBuffer[UserAchievement]()
    var nextId = 1L

    def newId(): UserAchievementId = { val id = nextId; nextId += 1; UserAchievementId(id) }

    new UserAchievementRepository[F] {
      override def create(userId: UserId, achievementId: AchievementId): F[UserAchievement] = {
        val now = Instant.now()
        val ua = UserAchievement(
          id = newId(),
          userId = userId,
          achievementId = achievementId,
          unlockedAt = now,
          createdAt = now,
          updatedAt = now
        )
        store += ua
        ua.pure[F]
      }

      override def findByUserId(userId: UserId): F[List[UserAchievement]] =
        store.filter(_.userId == userId).toList.pure[F]

      override def findByAchievementId(achievementId: AchievementId): F[List[UserAchievement]] =
        store.filter(_.achievementId == achievementId).toList.pure[F]

      override def delete(userId: UserId, achievementId: AchievementId): F[Int] = {
        val before = store.size
        store.filterInPlace(ua => !(ua.userId == userId && ua.achievementId == achievementId))
        val after = store.size
        (before - after).pure[F]
      }
    }.pure[F]
  }
}