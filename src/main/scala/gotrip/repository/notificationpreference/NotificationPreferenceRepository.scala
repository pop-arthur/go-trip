package gotrip.repository.notificationpreference

import cats.Applicative
import cats.effect.Concurrent
import skunk.Session
import cats.effect.Resource
import gotrip.domain.user.UserId
import gotrip.domain.notificationpreference.NotificationPreference

trait NotificationPreferenceRepository[F[_]]:
  def getByUserId(userId: UserId): F[Option[NotificationPreference]]
  def upsert(userId: UserId, isEnabled: Boolean): F[NotificationPreference]

object NotificationPreferenceRepository:
  def makeInMemory[F[_]: Applicative]: F[NotificationPreferenceRepository[F]] =
    InMemoryNotificationPreferenceRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): NotificationPreferenceRepository[F] =
    PostgresNotificationPreferenceRepository.make(sessionPool)