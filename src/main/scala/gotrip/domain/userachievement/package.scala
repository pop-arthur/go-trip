package gotrip.domain

import scala.annotation.targetName

package object userachievement {
  opaque type UserAchievementId = Long
  object UserAchievementId {
    def apply(value: Long): UserAchievementId = value
  }
  extension (id: UserAchievementId) {
    @targetName("userAchievementIdValue") def value: Long = id
  }
}