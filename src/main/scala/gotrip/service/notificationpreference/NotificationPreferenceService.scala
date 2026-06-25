package gotrip.service.notificationpreference

import gotrip.domain.user.UserId
import gotrip.domain.notificationpreference.NotificationPreference
import gotrip.repository.notificationpreference.NotificationPreferenceRepository

final class NotificationPreferenceService[F[_]](
  repo: NotificationPreferenceRepository[F]
):

  def getByUserId(userId: UserId): F[Option[NotificationPreference]] =
    repo.getByUserId(userId)

  def enable(userId: UserId): F[NotificationPreference] =
    repo.upsert(userId, isEnabled = true)

  def disable(userId: UserId): F[NotificationPreference] =
    repo.upsert(userId, isEnabled = false)

  def setStatus(userId: UserId, enabled: Boolean): F[NotificationPreference] =
    repo.upsert(userId, enabled)