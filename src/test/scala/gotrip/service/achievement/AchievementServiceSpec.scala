package gotrip.service.achievement

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement._
import gotrip.repository.achievement.AchievementRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class AchievementServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "AchievementService" should {
    "delegate listAll to repository" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.findAll _).expects().returning(IO.pure(List(achievement)))

      service.listAll().unsafeRunSync() shouldBe List(achievement)
    }

    "delegate findById to repository" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.findById _).expects(achievementId).returning(IO.pure(Some(achievement)))

      service.findById(achievementId).unsafeRunSync() shouldBe Some(achievement)
    }

    "delegate findByCode to repository" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.findByCode _).expects(achievementCode).returning(IO.pure(Some(achievement)))

      service.findByCode(achievementCode).unsafeRunSync() shouldBe Some(achievement)
    }

    "create an achievement and return it" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.create _).expects(where { (created: Achievement) =>
        created.code == achievement.code &&
        created.title == achievement.title &&
        created.description == achievement.description &&
        created.conditionType == achievement.conditionType &&
        created.conditionValue == achievement.conditionValue &&
        created.iconUrl == achievement.iconUrl
      }).returning(IO.pure(achievement))

      service.create(achievement).unsafeRunSync() shouldBe achievement
    }

    "update an existing achievement" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.update _).expects(where { (updated: Achievement) =>
        updated.id == achievementId &&
        updated.code == achievement.code &&
        updated.title == achievement.title &&
        updated.description == achievement.description &&
        updated.conditionType == achievement.conditionType &&
        updated.conditionValue == achievement.conditionValue &&
        updated.iconUrl == achievement.iconUrl
      }).returning(IO.pure(1))

      service.update(achievement).unsafeRunSync() shouldBe 1
    }

    "return 0 when updating a non-existent achievement" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.update _).expects(where { (updated: Achievement) =>
        updated.id == achievementId
      }).returning(IO.pure(0))

      service.update(achievement).unsafeRunSync() shouldBe 0
    }

    "delete an achievement" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.delete _).expects(achievementId).returning(IO.pure(1))

      service.delete(achievementId).unsafeRunSync() shouldBe 1
    }

    "return 0 when deleting a non-existent achievement" in {
      val repo = mock[AchievementRepository[IO]]
      val service = new AchievementService[IO](repo)

      (repo.delete _).expects(achievementId).returning(IO.pure(0))

      service.delete(achievementId).unsafeRunSync() shouldBe 0
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val achievementId = AchievementId(uuid("000000000001"))
  private val achievementCode = AchievementCode("FIRST_TRIP")
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
}