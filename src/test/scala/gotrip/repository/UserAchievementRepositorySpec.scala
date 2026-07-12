package gotrip.repository

import cats.effect.IO
import gotrip.repository.achievement.AchievementRepository
import gotrip.repository.user.UserRepository
import gotrip.repository.userachievement.UserAchievementRepository

final class UserAchievementRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("UserAchievementRepository creates and finds user achievements") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)
    val userAchievements = UserAchievementRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(50))
      achievement <- achievements.create(sampleAchievement(51, "FIRST_TRIP"))
      unlocked <- userAchievements.create(sampleUserAchievement(52, user.id, achievement.id))
      byUser <- userAchievements.findByUserId(user.id)
      byAchievement <- userAchievements.findByAchievementId(achievement.id)
    yield
      assertEquals(byUser, unlocked.toList)
      assertEquals(byAchievement, unlocked.toList)
  }

  repositoryTest("UserAchievementRepository rejects duplicate user achievements") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)
    val userAchievements = UserAchievementRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(50))
      achievement <- achievements.create(sampleAchievement(51, "FIRST_TRIP"))
      _ <- userAchievements.create(sampleUserAchievement(52, user.id, achievement.id))
      duplicate <- userAchievements.create(sampleUserAchievement(53, user.id, achievement.id)).attempt
    yield assert(duplicate.isLeft)
  }

  repositoryTest("UserAchievementRepository deletes user achievements") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val achievements = AchievementRepository.makePostgres[IO](sessionPool)
    val userAchievements = UserAchievementRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(50))
      achievement <- achievements.create(sampleAchievement(51, "FIRST_TRIP"))
      _ <- userAchievements.create(sampleUserAchievement(52, user.id, achievement.id))
      deleted <- userAchievements.delete(user.id, achievement.id)
      byUser <- userAchievements.findByUserId(user.id)
    yield
      assertEquals(deleted, 1)
      assertEquals(byUser, Nil)
  }
