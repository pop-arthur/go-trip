package gotrip.domain

import scala.annotation.targetName

package object notificationpreference {
  opaque type NotificationPreferenceId = Long
  object NotificationPreferenceId {
    def apply(value: Long): NotificationPreferenceId = value
  }
  extension (id: NotificationPreferenceId) {
    @targetName("notificationPreferenceIdValue") def value: Long = id
  }
}