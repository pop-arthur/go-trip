package gotrip.repository.notification

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.notification._
import java.time.Instant
import scala.collection.mutable

object InMemoryNotificationRepository {
  def make[F[_]: Applicative]: F[NotificationRepository[F]] = {
    val store = mutable.Map.empty[NotificationId, UserNotification]

    new NotificationRepository[F] {
      override def create(notification: UserNotification): F[UserNotification] = {
        store += (notification.id -> notification)
        notification.pure[F]
      }

      override def findById(id: NotificationId): F[Option[UserNotification]] =
        store.get(id).pure[F]

      override def findByUserId(userId: NotificationUserId, limit: Int = 50, offset: Int = 0): F[List[UserNotification]] =
        store.values.filter(_.userId == userId).toList.sortBy(_.sentAt)(using Ordering[Instant].reverse).drop(offset).take(limit).pure[F]

      override def markAsRead(id: NotificationId, updatedAt: Instant): F[Int] =
        store.get(id) match {
          case Some(notif) if !notif.isRead =>
            val updated = notif.copy(isRead = true, updatedAt = updatedAt)
            store += (id -> updated)
            1.pure[F]
          case Some(_) => 0.pure[F]
          case None    => 0.pure[F]
        }

      override def markAllAsRead(userId: NotificationUserId, updatedAt: Instant): F[Int] = {
        val toUpdate = store.values.filter(_.userId == userId).filterNot(_.isRead).toList
        toUpdate.foreach { n =>
          store += (n.id -> n.copy(isRead = true, updatedAt = updatedAt))
        }
        toUpdate.size.pure[F]
      }

      override def delete(id: NotificationId): F[Int] =
        store.remove(id).map(_ => 1).getOrElse(0).pure[F]

      override def deleteAllForUser(userId: NotificationUserId): F[Int] = {
        val toDelete = store.values.filter(_.userId == userId).map(_.id).toList
        toDelete.foreach(store.remove)
        toDelete.size.pure[F]
      }
    }.pure[F]
  }
}
