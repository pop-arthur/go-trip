package gotrip.service.userachievement

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.service.GeneratedData

trait UserAchievementService[F[_]] {
  def unlock(userId: UserId, achievementId: AchievementId): F[Option[UserAchievement]]
  def listByUser(userId: UserId): F[List[UserAchievement]]
  def listByAchievement(achievementId: AchievementId): F[List[UserAchievement]]
  def revoke(userId: UserId, achievementId: AchievementId): F[Int]
}

object UserAchievementService {
  def make[F[_]: Sync: Clock: GeneratedData](repo: UserAchievementRepository[F]): UserAchievementService[F] =
    new UserAchievementService[F] {
      override def unlock(userId: UserId, achievementId: AchievementId): F[Option[UserAchievement]] =
        for {
          id <- GeneratedData[F].newId()
          now <- GeneratedData[F].now()
          achievement <- repo.create(
            UserAchievement(
              id = UserAchievementId(id),
              userId = userId,
              achievementId = achievementId,
              unlockedAt = now,
              createdAt = now,
              updatedAt = now
            )
          )
        } yield achievement

      override def listByUser(userId: UserId): F[List[UserAchievement]] =
        repo.findByUserId(userId)

      override def listByAchievement(achievementId: AchievementId): F[List[UserAchievement]] =
        repo.findByAchievementId(achievementId)

      override def revoke(userId: UserId, achievementId: AchievementId): F[Int] =
        repo.delete(userId, achievementId)
    }
}