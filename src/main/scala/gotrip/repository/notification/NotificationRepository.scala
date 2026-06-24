package gotrip.repository.notification

import cats.Applicative
import cats.effect.Concurrent
import skunk.Session
import cats.effect.Resource
import gotrip.domain.notification.{UserNotification, NotificationId, NotificationUserId}

trait NotificationRepository[F[_]]:
  def create(notification: UserNotification): F[UserNotification]
  def findById(id: NotificationId): F[Option[UserNotification]]
  def findByUserId(userId: NotificationUserId, limit: Int = 50, offset: Int = 0): F[List[UserNotification]]
  def markAsRead(id: NotificationId): F[Int]
  def markAllAsRead(userId: NotificationUserId): F[Int]
  def delete(id: NotificationId): F[Int]
  def deleteAllForUser(userId: NotificationUserId): F[Int]

object NotificationRepository:
  def makeInMemory[F[_]: Applicative]: F[NotificationRepository[F]] =
    InMemoryNotificationRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): NotificationRepository[F] =
    PostgresNotificationRepository.make(sessionPool)