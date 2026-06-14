package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

case class NotificationPreference(
    id: Long,
    userId: Long,
    isEnabled: Boolean,
    createdAt: Instant,
    updatedAt: Instant
)

object NotificationPreference {
  implicit val npRead: Read[NotificationPreference] = Read[(Long, Long, Boolean, Instant, Instant)].map {
    case (id, uid, enabled, createdAt, updatedAt) => NotificationPreference(id, uid, enabled, createdAt, updatedAt)
  }
}