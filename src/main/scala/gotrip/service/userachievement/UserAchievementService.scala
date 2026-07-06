package gotrip.service.userachievement

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.service.GeneratedData

final class UserAchievementService[F[_]: Sync: Clock: GeneratedData](
  repo: UserAchievementRepository[F]
):

  def unlock(userId: UserId, achievementId: AchievementId): F[UserAchievement] =
    for
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
    yield achievement

  def listByUser(userId: UserId): F[List[UserAchievement]] =
    repo.findByUserId(userId)

  def listByAchievement(achievementId: AchievementId): F[List[UserAchievement]] =
    repo.findByAchievementId(achievementId)

  def revoke(userId: UserId, achievementId: AchievementId): F[Int] =
    repo.delete(userId, achievementId)
