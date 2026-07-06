package gotrip.service.notification

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.notification._
import gotrip.repository.notification.NotificationRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class NotificationServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "NotificationService" should {
    "send a notification (create) and return it" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.create _).expects(where { (n: UserNotification) =>
        n.userId == userId &&
        n.orderId == NotificationOrderId(Some(orderUuid)) &&
        n.notificationType == NotificationType.StatusChange &&
        n.title.value == "Status changed" &&
        n.body.value == "Your order status changed" &&
        n.isRead == false
      }).returning(IO.pure(notification))

      val result = service.send(
        userId,
        NotificationType.StatusChange,
        NotificationTitle("Status changed"),
        NotificationBody("Your order status changed"),
        NotificationOrderId(Some(orderUuid))
      )
      result.unsafeRunSync() shouldBe notification
    }

    "find notification by id" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.findById _).expects(notificationId).returning(IO.pure(Some(notification)))

      service.findById(notificationId).unsafeRunSync() shouldBe Some(notification)
    }

    "list notifications by user with default limit" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.findByUserId _).expects(userId, 50, 0).returning(IO.pure(List(notification)))

      service.listByUser(userId).unsafeRunSync() shouldBe List(notification)
    }

    "list notifications by user with custom limit and offset" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.findByUserId _).expects(userId, 10, 20).returning(IO.pure(List(notification)))

      service.listByUser(userId, 10, 20).unsafeRunSync() shouldBe List(notification)
    }

    "mark a notification as read" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.markAsRead _).expects(where { (id: NotificationId, t: Instant) =>
        id == notificationId
      }).returning(IO.pure(1))

      service.markAsRead(notificationId).unsafeRunSync() shouldBe 1
    }

    "mark all notifications as read for a user" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.markAllAsRead _).expects(where { (uid: NotificationUserId, t: Instant) =>
        uid == userId
      }).returning(IO.pure(5))

      service.markAllAsRead(userId).unsafeRunSync() shouldBe 5
    }

    "delete a notification" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.delete _).expects(notificationId).returning(IO.pure(1))

      service.delete(notificationId).unsafeRunSync() shouldBe 1
    }

    "delete all notifications for a user" in {
      val repo = mock[NotificationRepository[IO]]
      val service = new NotificationService[IO](repo)

      (repo.deleteAllForUser _).expects(userId).returning(IO.pure(3))

      service.deleteAllForUser(userId).unsafeRunSync() shouldBe 3
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = NotificationUserId(uuid("000000000001"))
  private val notificationId = NotificationId(uuid("000000000100"))
  private val orderUuid = uuid("000000000123")
  private val notification = UserNotification(
    id = notificationId,
    userId = userId,
    orderId = NotificationOrderId(Some(orderUuid)),
    notificationType = NotificationType.StatusChange,
    title = NotificationTitle("Status changed"),
    body = NotificationBody("Your order status changed"),
    isRead = false,
    sentAt = Instant.now(),
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )
}