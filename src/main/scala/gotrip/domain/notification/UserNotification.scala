package gotrip.domain.notification

import java.time.Instant

final case class UserNotification(
  id: NotificationId,
  userId: NotificationUserId,
  orderId: NotificationOrderId,
  notificationType: NotificationType,
  title: NotificationTitle,
  body: NotificationBody,
  isRead: Boolean,
  sentAt: Instant,
  createdAt: Instant,
  updatedAt: Instant
)