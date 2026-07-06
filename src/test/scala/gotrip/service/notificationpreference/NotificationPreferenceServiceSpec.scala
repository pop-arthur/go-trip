package gotrip.service.notificationpreference

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.notificationpreference.{NotificationPreference, NotificationPreferenceId}
import gotrip.domain.user.UserId
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class NotificationPreferenceServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "NotificationPreferenceService" should {
    "get preference for a user" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      (repo.getByUserId _).expects(userId).returning(IO.pure(Some(preference)))

      service.getByUserId(userId).unsafeRunSync() shouldBe Some(preference)
    }

    "enable notifications (upsert)" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      // Сервис сначала вызывает getByUserId для проверки
      (repo.getByUserId _).expects(userId).returning(IO.pure(None)).once()
      (repo.upsert _).expects(where { (pref: NotificationPreference) =>
        pref.userId == userId && pref.isEnabled == true
      }).returning(IO.pure(preference)).once()

      service.enable(userId).unsafeRunSync() shouldBe preference
    }

    "disable notifications (upsert)" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      (repo.getByUserId _).expects(userId).returning(IO.pure(None)).once()
      (repo.upsert _).expects(where { (pref: NotificationPreference) =>
        pref.userId == userId && pref.isEnabled == false
      }).returning(IO.pure(preference.copy(isEnabled = false))).once()

      service.disable(userId).unsafeRunSync() shouldBe preference.copy(isEnabled = false)
    }

    "set status to enabled" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      (repo.getByUserId _).expects(userId).returning(IO.pure(None)).once()
      (repo.upsert _).expects(where { (pref: NotificationPreference) =>
        pref.userId == userId && pref.isEnabled == true
      }).returning(IO.pure(preference)).once()

      service.setStatus(userId, enabled = true).unsafeRunSync() shouldBe preference
    }

    "set status to disabled" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      (repo.getByUserId _).expects(userId).returning(IO.pure(None)).once()
      (repo.upsert _).expects(where { (pref: NotificationPreference) =>
        pref.userId == userId && pref.isEnabled == false
      }).returning(IO.pure(preference.copy(isEnabled = false))).once()

      service.setStatus(userId, enabled = false).unsafeRunSync() shouldBe preference.copy(isEnabled = false)
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = UserId(uuid("000000000001"))
  private val preference = NotificationPreference(
    id = NotificationPreferenceId(uuid("000000000001")),
    userId = userId,
    isEnabled = true,
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )
}