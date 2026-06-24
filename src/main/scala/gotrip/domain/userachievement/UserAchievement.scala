package gotrip.domain.userachievement

import java.time.Instant
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId

final case class UserAchievement(
  id: UserAchievementId,
  userId: UserId,
  achievementId: AchievementId,
  unlockedAt: Instant,
  createdAt: Instant,
  updatedAt: Instant
)