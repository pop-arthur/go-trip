package gotrip.service.userachievement

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement.AchievementId
import gotrip.domain.user.UserId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.userachievement.UserAchievementRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class UserAchievementServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport {

  "UserAchievementService" should {
    "unlock an achievement (create)" in {
      val repo = mock[UserAchievementRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      expectGeneratedId(generatedData, userAchievementUuid)
      expectGeneratedNow(generatedData, generatedAt)
      repo.create.expects(userAchievement).returning(IO.pure(Some(userAchievement)))

      service.unlock(userId, achievementId).unsafeRunSync() shouldBe Some(userAchievement)
    }

    "list achievements by user" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      repo.findByUserId.expects(userId).returning(IO.pure(List(userAchievement)))

      service.listByUser(userId).unsafeRunSync() shouldBe List(userAchievement)
    }

    "list achievements by achievement id" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      repo.findByAchievementId.expects(achievementId).returning(IO.pure(List(userAchievement)))

      service.listByAchievement(achievementId).unsafeRunSync() shouldBe List(userAchievement)
    }

    "revoke an achievement (delete)" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      repo.delete.expects(userId, achievementId).returning(IO.pure(1))

      service.revoke(userId, achievementId).unsafeRunSync() shouldBe 1
    }

    "return 0 if revoking non-existent pair" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      repo.delete.expects(userId, achievementId).returning(IO.pure(0))

      service.revoke(userId, achievementId).unsafeRunSync() shouldBe 0
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: UserAchievementRepository[IO],
    generatedData: GeneratedData[IO]
  ): UserAchievementService[IO] =
    given GeneratedData[IO] = generatedData
    new UserAchievementService[IO](repository)

  private val userId = UserId(uuid("000000000001"))
  private val achievementId = AchievementId(uuid("000000000010"))
  private val userAchievementUuid = uuid("000000000100")
  private val userAchievementId = UserAchievementId(userAchievementUuid)
  private val generatedAt = Instant.parse("2026-06-01T10:00:00Z")
  private val userAchievement = UserAchievement(
    id = userAchievementId,
    userId = userId,
    achievementId = achievementId,
    unlockedAt = generatedAt,
    createdAt = generatedAt,
    updatedAt = generatedAt
  )
}
