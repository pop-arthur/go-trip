package gotrip.service.notificationpreference

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.notificationpreference.{NotificationPreference, NotificationPreferenceId}
import gotrip.domain.user.UserId
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class NotificationPreferenceServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport {

  "NotificationPreferenceService" should {
    "get preference for a user" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val service = new NotificationPreferenceService[IO](repo)

      repo.getByUserId.expects(userId).returning(IO.pure(Some(preference)))

      service.getByUserId(userId).unsafeRunSync() shouldBe Some(preference)
    }

    "enable notifications (upsert)" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      // Сервис сначала вызывает getByUserId для проверки
      repo.getByUserId.expects(userId).returning(IO.pure(None)).once()
      expectGeneratedNow(generatedData, generatedAt)
      expectGeneratedId(generatedData, preferenceUuid)
      repo.upsert.expects(preference).returning(IO.pure(preference)).once()

      service.enable(userId).unsafeRunSync() shouldBe preference
    }

    "disable notifications (upsert)" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)
      val disabled = preference.copy(isEnabled = false)

      repo.getByUserId.expects(userId).returning(IO.pure(None)).once()
      expectGeneratedNow(generatedData, generatedAt)
      expectGeneratedId(generatedData, preferenceUuid)
      repo.upsert.expects(disabled).returning(IO.pure(disabled)).once()

      service.disable(userId).unsafeRunSync() shouldBe disabled
    }

    "set status to enabled" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      repo.getByUserId.expects(userId).returning(IO.pure(None)).once()
      expectGeneratedNow(generatedData, generatedAt)
      expectGeneratedId(generatedData, preferenceUuid)
      repo.upsert.expects(preference).returning(IO.pure(preference)).once()

      service.setStatus(userId, enabled = true).unsafeRunSync() shouldBe preference
    }

    "set status to disabled" in {
      val repo = mock[NotificationPreferenceRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)
      val disabled = preference.copy(isEnabled = false)

      repo.getByUserId.expects(userId).returning(IO.pure(None)).once()
      expectGeneratedNow(generatedData, generatedAt)
      expectGeneratedId(generatedData, preferenceUuid)
      repo.upsert.expects(disabled).returning(IO.pure(disabled)).once()

      service.setStatus(userId, enabled = false).unsafeRunSync() shouldBe disabled
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: NotificationPreferenceRepository[IO],
    generatedData: GeneratedData[IO]
  ): NotificationPreferenceService[IO] =
    given GeneratedData[IO] = generatedData
    new NotificationPreferenceService[IO](repository)

  private val userId = UserId(uuid("000000000001"))
  private val preferenceUuid = uuid("000000000001")
  private val preferenceId = NotificationPreferenceId(preferenceUuid)
  private val generatedAt = Instant.parse("2026-06-01T10:00:00Z")
  private val preference = NotificationPreference(
    id = preferenceId,
    userId = userId,
    isEnabled = true,
    createdAt = generatedAt,
    updatedAt = generatedAt
  )
}
