package gotrip.repository.userachievement

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.UserAchievement
import scala.collection.mutable

object InMemoryUserAchievementRepository {
  def make[F[_]: Applicative]: F[UserAchievementRepository[F]] = {
    val store = mutable.ListBuffer[UserAchievement]()

    new UserAchievementRepository[F] {
      override def create(userAchievement: UserAchievement): F[Option[UserAchievement]] = {
        val exists = store.exists(ua => ua.userId == userAchievement.userId && ua.achievementId == userAchievement.achievementId)
        if (exists) {
          none[UserAchievement].pure[F]
        } else {
          store += userAchievement
          userAchievement.some.pure[F]
        }
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