package gotrip.service.achievement

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement.*
import gotrip.repository.achievement.AchievementRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class AchievementServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport:

  "AchievementService" should {
    "delegate listAll to repository" in {
      val repository = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repository)

      (() => repository.findAll()).expects().returning(IO.pure(List(achievement)))

      service.listAll().unsafeRunSync() shouldBe List(achievement)
    }

    "delegate findById to repository" in {
      val repository = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repository)

      repository.findById.expects(achievementId).returning(IO.pure(Some(achievement)))

      service.findById(achievementId).unsafeRunSync() shouldBe Some(achievement)
    }

    "delegate findByCode to repository" in {
      val repository = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repository)

      repository.findByCode.expects(achievementCode).returning(IO.pure(Some(achievement)))

      service.findByCode(achievementCode).unsafeRunSync() shouldBe Some(achievement)
    }

    "create an achievement and return it" in {
      val repository = mock[AchievementRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)
      val created = achievement.copy(createdAt = generatedAt, updatedAt = generatedAt)

      expectGeneratedId(generatedData, achievementId.value)
      expectGeneratedNow(generatedData, generatedAt)
      repository.create.expects(created).returning(IO.pure(achievement))

      service.create(achievement).unsafeRunSync() shouldBe achievement
    }

    "update an existing achievement" in {
      val repository = mock[AchievementRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)
      val updated = achievement.copy(updatedAt = generatedAt)

      expectGeneratedNow(generatedData, generatedAt)
      repository.update.expects(updated).returning(IO.pure(1))

      service.update(achievement).unsafeRunSync() shouldBe 1
    }

    "return 0 when updating a non-existent achievement" in {
      val repository = mock[AchievementRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)
      val updated = achievement.copy(updatedAt = generatedAt)

      expectGeneratedNow(generatedData, generatedAt)
      repository.update.expects(updated).returning(IO.pure(0))

      service.update(achievement).unsafeRunSync() shouldBe 0
    }

    "delete an achievement" in {
      val repository = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repository)

      repository.delete.expects(achievementId).returning(IO.pure(1))

      service.delete(achievementId).unsafeRunSync() shouldBe 1
    }

    "return 0 when deleting a non-existent achievement" in {
      val repository = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repository)

      repository.delete.expects(achievementId).returning(IO.pure(0))

      service.delete(achievementId).unsafeRunSync() shouldBe 0
    }
  }

  private def serviceWith(
    repository: AchievementRepository[IO],
    generatedData: GeneratedData[IO]
  ): AchievementService[IO] =
    given GeneratedData[IO] = generatedData
    new AchievementService[IO](repository)

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val achievementId = AchievementId(uuid("000000000001"))
  private val achievementCode = AchievementCode("FIRST_TRIP")
  private val generatedAt = Instant.parse("2024-01-01T00:00:00Z")
  private val achievement = Achievement(
    id = achievementId,
    code = achievementCode,
    title = AchievementTitle("First Trip"),
    description = AchievementDescription(Some("Complete your first trip")),
    conditionType = AchievementConditionType.TripsCount,
    conditionValue = 1,
    iconUrl = AchievementIconUrl(Some("https://example.com/icon.svg")),
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )
