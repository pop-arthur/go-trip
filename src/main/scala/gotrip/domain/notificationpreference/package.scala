package gotrip.domain

import scala.annotation.targetName
import java.util.UUID

package object notificationpreference {
  opaque type NotificationPreferenceId = UUID
  object NotificationPreferenceId {
    def apply(value: UUID): NotificationPreferenceId = value
  }
  extension (id: NotificationPreferenceId) {
    @targetName("notificationPreferenceIdValue") def value: UUID = id
  }
}
