package gotrip.repository.notificationpreference

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user.UserId
import gotrip.domain.notificationpreference.{NotificationPreference, NotificationPreferenceId}
import java.time.Instant
import scala.collection.mutable

object InMemoryNotificationPreferenceRepository {
  def make[F[_]: Applicative]: F[NotificationPreferenceRepository[F]] = {
    val store = mutable.Map.empty[UserId, NotificationPreference]
    var nextId = 1L

    def newId(): NotificationPreferenceId = { val id = nextId; nextId += 1; NotificationPreferenceId(id) }

    new NotificationPreferenceRepository[F] {
      override def getByUserId(userId: UserId): F[Option[NotificationPreference]] =
        store.get(userId).pure[F]

      override def upsert(userId: UserId, isEnabled: Boolean): F[NotificationPreference] = {
        val now = Instant.now()
        val pref = store.get(userId) match {
          case Some(existing) =>
            existing.copy(isEnabled = isEnabled, updatedAt = now)
          case None =>
            NotificationPreference(
              id = newId(),
              userId = userId,
              isEnabled = isEnabled,
              createdAt = now,
              updatedAt = now
            )
        }
        store += (userId -> pref)
        pref.pure[F]
      }
    }.pure[F]
  }
}