package gotrip.domain.achievement

import java.time.Instant

final case class Achievement(
  id: AchievementId,
  code: AchievementCode,
  title: AchievementTitle,
  description: AchievementDescription,
  conditionType: AchievementConditionType,
  conditionValue: Int,
  iconUrl: AchievementIconUrl,
  createdAt: Instant,
  updatedAt: Instant
)