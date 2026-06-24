package gotrip.service.notification

import gotrip.domain.notification._
import gotrip.repository.notification.NotificationRepository

final class NotificationService[F[_]](
  repo: NotificationRepository[F]
):

  def send(
    userId: NotificationUserId,
    notificationType: NotificationType,
    title: NotificationTitle,
    body: NotificationBody,
    orderId: NotificationOrderId = NotificationOrderId(None)
  ): F[UserNotification] =
    val notification = UserNotification(
      id = NotificationId(0L),
      userId = userId,
      orderId = orderId,
      notificationType = notificationType,
      title = title,
      body = body,
      isRead = false,
      sentAt = java.time.Instant.now(),
      createdAt = java.time.Instant.now(),
      updatedAt = java.time.Instant.now()
    )
    repo.create(notification)

  def findById(id: NotificationId): F[Option[UserNotification]] = repo.findById(id)

  def listByUser(userId: NotificationUserId, limit: Int = 50, offset: Int = 0): F[List[UserNotification]] =
    repo.findByUserId(userId, limit, offset)

  def markAsRead(id: NotificationId): F[Int] = repo.markAsRead(id)
  def markAllAsRead(userId: NotificationUserId): F[Int] = repo.markAllAsRead(userId)
  def delete(id: NotificationId): F[Int] = repo.delete(id)
  def deleteAllForUser(userId: NotificationUserId): F[Int] = repo.deleteAllForUser(userId)