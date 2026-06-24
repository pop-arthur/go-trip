package gotrip.domain.notificationpreference

import java.time.Instant
import gotrip.domain.user.UserId

final case class NotificationPreference(
  id: NotificationPreferenceId,
  userId: UserId,
  isEnabled: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)