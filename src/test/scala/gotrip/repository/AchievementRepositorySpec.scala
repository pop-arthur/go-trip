package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.achievement.*
import gotrip.repository.achievement.AchievementRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class AchievementRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "AchievementRepository" should {
    "create, find, update, and delete achievements" in {
      val achievements = AchievementRepository.makePostgres[IO](sessionPool)
      val achievement = achievements.create(sampleAchievement(41, "FIRST_TRIP")).unsafeRunSync()

      achievements.findAll().unsafeRunSync() shouldBe List(achievement)
      achievements.findById(achievement.id).unsafeRunSync() shouldBe Some(achievement)
      achievements.findByCode(achievement.code).unsafeRunSync() shouldBe Some(achievement)
      achievements.update(achievement.copy(title = AchievementTitle("First completed trip"), updatedAt = t(42))).unsafeRunSync() shouldBe 1
      achievements.delete(achievement.id).unsafeRunSync() shouldBe 1
    }
  }
