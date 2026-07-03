package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.notification.NotificationUserId
import gotrip.domain.user.*
import gotrip.repository.notification.NotificationRepository
import gotrip.repository.user.UserRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class NotificationRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "NotificationRepository" should {
    "create, find, page by user, mark read, and delete notifications" in {
      val users = UserRepository.makePostgres[IO](sessionPool)
      val notifications = NotificationRepository.makePostgres[IO](sessionPool)
      val user = users.create(sampleUser(110)).unsafeRunSync()
      val notificationUserId = NotificationUserId(user.id.value)

      val oldNotification = notifications.create(sampleNotification(111, user.id, sentAt = t(111))).unsafeRunSync()
      val newNotification = notifications.create(sampleNotification(112, user.id, sentAt = t(112))).unsafeRunSync()
      notifications.findById(oldNotification.id).unsafeRunSync() shouldBe Some(oldNotification)
      notifications.findByUserId(notificationUserId, limit = 1, offset = 0).unsafeRunSync() shouldBe List(newNotification)
      notifications.findByUserId(notificationUserId, limit = 1, offset = 1).unsafeRunSync() shouldBe List(oldNotification)
      notifications.markAsRead(oldNotification.id, t(113)).unsafeRunSync() shouldBe 1
      notifications.findById(oldNotification.id).unsafeRunSync().map(_.isRead) shouldBe Some(true)
      notifications.markAllAsRead(notificationUserId, t(114)).unsafeRunSync() shouldBe 1
      notifications.delete(newNotification.id).unsafeRunSync() shouldBe 1
      notifications.deleteAllForUser(notificationUserId).unsafeRunSync() shouldBe 1
    }
  }
