package gotrip.service.notification

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.notification._
import gotrip.repository.notification.NotificationRepository
import gotrip.service.GeneratedData

final class NotificationService[F[_]: Sync: Clock: GeneratedData](
  repo: NotificationRepository[F]
):

  def send(
    userId: NotificationUserId,
    notificationType: NotificationType,
    title: NotificationTitle,
    body: NotificationBody,
    orderId: NotificationOrderId = NotificationOrderId(None)
  ): F[UserNotification] =
    for
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
    yield created

  def findById(id: NotificationId): F[Option[UserNotification]] = repo.findById(id)

  def listByUser(userId: NotificationUserId, limit: Int = 50, offset: Int = 0): F[List[UserNotification]] =
    repo.findByUserId(userId, limit, offset)

  def markAsRead(id: NotificationId): F[Int] =
    GeneratedData[F].now().flatMap(now => repo.markAsRead(id, now))

  def markAllAsRead(userId: NotificationUserId): F[Int] =
    GeneratedData[F].now().flatMap(now => repo.markAllAsRead(userId, now))
  def delete(id: NotificationId): F[Int] = repo.delete(id)
  def deleteAllForUser(userId: NotificationUserId): F[Int] = repo.deleteAllForUser(userId)
