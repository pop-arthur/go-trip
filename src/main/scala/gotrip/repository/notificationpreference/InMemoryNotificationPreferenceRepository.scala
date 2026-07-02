package gotrip.repository.notificationpreference

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user.UserId
import gotrip.domain.notificationpreference.NotificationPreference
import scala.collection.mutable

object InMemoryNotificationPreferenceRepository {
  def make[F[_]: Applicative]: F[NotificationPreferenceRepository[F]] = {
    val store = mutable.Map.empty[UserId, NotificationPreference]

    new NotificationPreferenceRepository[F] {
      override def getByUserId(userId: UserId): F[Option[NotificationPreference]] =
        store.get(userId).pure[F]

      override def upsert(preference: NotificationPreference): F[NotificationPreference] = {
        store += (preference.userId -> preference)
        preference.pure[F]
      }
    }.pure[F]
  }
}
