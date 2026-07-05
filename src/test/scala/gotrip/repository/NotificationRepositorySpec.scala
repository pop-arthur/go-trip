package gotrip.repository

import cats.effect.IO
import gotrip.domain.notification.NotificationUserId
import gotrip.domain.user.*
import gotrip.repository.notification.NotificationRepository
import gotrip.repository.user.UserRepository

final class NotificationRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("NotificationRepository creates and finds notifications") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notification <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      byId <- notifications.findById(notification.id)
    yield assertEquals(byId, Some(notification))
  }

  repositoryTest("NotificationRepository lists notifications newest first with limit and offset") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notificationUserId = NotificationUserId(user.id.value)
      oldNotification <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      newNotification <- notifications.create(sampleNotification(112, user.id, sentAt = t(112)))
      firstPage <- notifications.findByUserId(notificationUserId, limit = 1, offset = 0)
      secondPage <- notifications.findByUserId(notificationUserId, limit = 1, offset = 1)
    yield
      assertEquals(firstPage, List(newNotification))
      assertEquals(secondPage, List(oldNotification))
  }

  repositoryTest("NotificationRepository marks one notification as read") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notification <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      markedOne <- notifications.markAsRead(notification.id, t(113))
      afterRead <- notifications.findById(notification.id)
    yield
      assertEquals(markedOne, 1)
      assertEquals(afterRead.map(_.isRead), Some(true))
  }

  repositoryTest("NotificationRepository marks all notifications as read for a user") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notificationUserId = NotificationUserId(user.id.value)
      _ <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      _ <- notifications.create(sampleNotification(112, user.id, sentAt = t(112)))
      markedAll <- notifications.markAllAsRead(notificationUserId, t(114))
    yield assertEquals(markedAll, 2)
  }

  repositoryTest("NotificationRepository deletes one notification") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notification <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      deleted <- notifications.delete(notification.id)
      afterDelete <- notifications.findById(notification.id)
    yield
      assertEquals(deleted, 1)
      assertEquals(afterDelete, None)
  }

  repositoryTest("NotificationRepository deletes all notifications for a user") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val notifications = NotificationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(110))
      notificationUserId = NotificationUserId(user.id.value)
      _ <- notifications.create(sampleNotification(111, user.id, sentAt = t(111)))
      _ <- notifications.create(sampleNotification(112, user.id, sentAt = t(112)))
      deletedAll <- notifications.deleteAllForUser(notificationUserId)
      remaining <- notifications.findByUserId(notificationUserId, limit = 10, offset = 0)
    yield
      assertEquals(deletedAll, 2)
      assertEquals(remaining, Nil)
  }
