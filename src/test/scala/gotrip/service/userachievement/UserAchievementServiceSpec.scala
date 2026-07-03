package gotrip.service.userachievement

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement.AchievementId
import gotrip.domain.user.UserId
import gotrip.domain.userachievement.{UserAchievement, UserAchievementId}
import gotrip.repository.userachievement.UserAchievementRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class UserAchievementServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "UserAchievementService" should {
    "unlock an achievement (create)" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      (repo.create _).expects(where { (ua: UserAchievement) =>
        ua.userId == userId && ua.achievementId == achievementId
      }).returning(IO.pure(userAchievement))

      service.unlock(userId, achievementId).unsafeRunSync() shouldBe userAchievement
    }

    "list achievements by user" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      (repo.findByUserId _).expects(userId).returning(IO.pure(List(userAchievement)))

      service.listByUser(userId).unsafeRunSync() shouldBe List(userAchievement)
    }

    "list achievements by achievement id" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      (repo.findByAchievementId _).expects(achievementId).returning(IO.pure(List(userAchievement)))

      service.listByAchievement(achievementId).unsafeRunSync() shouldBe List(userAchievement)
    }

    "revoke an achievement (delete)" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      (repo.delete _).expects(where { (uid: UserId, aid: AchievementId) =>
        uid == userId && aid == achievementId
      }).returning(IO.pure(1))

      service.revoke(userId, achievementId).unsafeRunSync() shouldBe 1
    }

    "return 0 if revoking non-existent pair" in {
      val repo = mock[UserAchievementRepository[IO]]
      val service = new UserAchievementService[IO](repo)

      (repo.delete _).expects(where { (uid: UserId, aid: AchievementId) =>
        uid == userId && aid == achievementId
      }).returning(IO.pure(0))

      service.revoke(userId, achievementId).unsafeRunSync() shouldBe 0
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = UserId(uuid("000000000001"))
  private val achievementId = AchievementId(uuid("000000000010"))
  private val userAchievement = UserAchievement(
    id = UserAchievementId(uuid("000000000100")),
    userId = userId,
    achievementId = achievementId,
    unlockedAt = Instant.now(),
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )
}