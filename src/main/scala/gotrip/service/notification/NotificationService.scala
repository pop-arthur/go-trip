package gotrip.service.notification

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.notification._
import gotrip.repository.notification.NotificationRepository
import gotrip.service.GeneratedData

trait NotificationService[F[_]] {
  def send(
    userId: NotificationUserId,
    notificationType: NotificationType,
    title: NotificationTitle,
    body: NotificationBody,
    orderId: NotificationOrderId
  ): F[UserNotification]
  def findById(id: NotificationId): F[Option[UserNotification]]
  def listByUser(userId: NotificationUserId, limit: Int, offset: Int): F[List[UserNotification]]
  def markAsRead(id: NotificationId): F[Int]
  def markAllAsRead(userId: NotificationUserId): F[Int]
  def delete(id: NotificationId): F[Int]
  def deleteAllForUser(userId: NotificationUserId): F[Int]
}

object NotificationService {
  def make[F[_]: Sync: Clock: GeneratedData](repo: NotificationRepository[F]): NotificationService[F] =
    new NotificationService[F] {
      override def send(
        userId: NotificationUserId,
        notificationType: NotificationType,
        title: NotificationTitle,
        body: NotificationBody,
        orderId: NotificationOrderId = NotificationOrderId(None)
      ): F[UserNotification] =
        for {
          id <- GeneratedData[F].newId()
          now <- GeneratedData[F].now()
          notification = UserNotification(
            id = NotificationId(id),
            userId = userId,
            orderId = orderId,
            notificationType = notificationType,
            title = title,
            body = body,
            isRead = false,
            sentAt = now,
            createdAt = now,
            updatedAt = now
          )
          created <- repo.create(notification)
        } yield created

      override def findById(id: NotificationId): F[Option[UserNotification]] = repo.findById(id)
      override def listByUser(userId: NotificationUserId, limit: Int, offset: Int): F[List[UserNotification]] =
        repo.findByUserId(userId, limit, offset)
      override def markAsRead(id: NotificationId): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.markAsRead(id, now))
      override def markAllAsRead(userId: NotificationUserId): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.markAllAsRead(userId, now))
      override def delete(id: NotificationId): F[Int] = repo.delete(id)
      override def deleteAllForUser(userId: NotificationUserId): F[Int] = repo.deleteAllForUser(userId)
    }
}