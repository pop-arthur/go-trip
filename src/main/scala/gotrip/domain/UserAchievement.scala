package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

case class UserAchievement(
    id: Long,
    userId: Long,
    achievementId: Long,
    unlockedAt: Instant
)

object UserAchievement {
  implicit val uaRead: Read[UserAchievement] = Read[(Long, Long, Long, Instant)].map {
    case (id, uid, aid, unlockedAt) => UserAchievement(id, uid, aid, unlockedAt)
  }
}