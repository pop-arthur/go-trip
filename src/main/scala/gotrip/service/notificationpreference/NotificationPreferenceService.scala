package gotrip.service.notificationpreference

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.user.UserId
import gotrip.domain.notificationpreference.{NotificationPreference, NotificationPreferenceId}
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.service.GeneratedData

final class NotificationPreferenceService[F[_]: Sync: Clock](
  repo: NotificationPreferenceRepository[F]
):

  def getByUserId(userId: UserId): F[Option[NotificationPreference]] =
    repo.getByUserId(userId)

  def enable(userId: UserId): F[NotificationPreference] =
    setStatus(userId, enabled = true)

  def disable(userId: UserId): F[NotificationPreference] =
    setStatus(userId, enabled = false)

  def setStatus(userId: UserId, enabled: Boolean): F[NotificationPreference] =
    for
      existing <- repo.getByUserId(userId)
      now <- GeneratedData.now[F]
      preference <- existing match
        case Some(current) =>
          repo.upsert(current.copy(isEnabled = enabled, updatedAt = now))
        case None =>
          GeneratedData.newId[F].flatMap { id =>
            repo.upsert(
              NotificationPreference(
                id = NotificationPreferenceId(id),
                userId = userId,
                isEnabled = enabled,
                createdAt = now,
                updatedAt = now
              )
            )
          }
    yield preference
