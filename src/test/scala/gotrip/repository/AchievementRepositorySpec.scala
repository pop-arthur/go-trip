package gotrip.repository

import cats.effect.IO
import gotrip.domain.achievement.*
import gotrip.repository.achievement.AchievementRepository

final class AchievementRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("AchievementRepository creates and finds achievements") {
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)

    for
      achievement <- achievements.create(sampleAchievement(41, "FIRST_TRIP"))
      all <- achievements.findAll()
      byId <- achievements.findById(achievement.id)
      byCode <- achievements.findByCode(achievement.code)
    yield
      assertEquals(all, List(achievement))
      assertEquals(byId, Some(achievement))
      assertEquals(byCode, Some(achievement))
  }

  repositoryTest("AchievementRepository updates achievements") {
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)

    for
      achievement <- achievements.create(sampleAchievement(42, "FIRST_TRIP"))
      rows <- achievements.update(achievement.copy(title = AchievementTitle("First completed trip"), updatedAt = t(43)))
      updated <- achievements.findById(achievement.id)
    yield
      assertEquals(rows, 1)
      assertEquals(updated.map(_.title), Some(AchievementTitle("First completed trip")))
  }

  repositoryTest("AchievementRepository deletes achievements") {
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)

    for
      achievement <- achievements.create(sampleAchievement(44, "FIRST_TRIP"))
      rows <- achievements.delete(achievement.id)
      deleted <- achievements.findById(achievement.id)
    yield
      assertEquals(rows, 1)
      assertEquals(deleted, None)
  }
