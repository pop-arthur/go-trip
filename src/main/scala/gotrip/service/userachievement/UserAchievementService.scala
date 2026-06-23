package gotrip.service.userachievement

import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.UserAchievement
import gotrip.repository.userachievement.UserAchievementRepository

final class UserAchievementService[F[_]](
  repo: UserAchievementRepository[F]
):

  def unlock(userId: UserId, achievementId: AchievementId): F[UserAchievement] =
    repo.create(userId, achievementId)

  def listByUser(userId: UserId): F[List[UserAchievement]] =
    repo.findByUserId(userId)

  def listByAchievement(achievementId: AchievementId): F[List[UserAchievement]] =
    repo.findByAchievementId(achievementId)

  def revoke(userId: UserId, achievementId: AchievementId): F[Int] =
    repo.delete(userId, achievementId)