package gotrip.domain

import scala.annotation.targetName
import java.util.UUID

package object userachievement {
  opaque type UserAchievementId = UUID
  object UserAchievementId {
    def apply(value: UUID): UserAchievementId = value
  }
  extension (id: UserAchievementId) {
    @targetName("userAchievementIdValue") def value: UUID = id
  }
}
