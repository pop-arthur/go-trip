package gotrip.repository

import cats.effect.IO
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.user.UserRepository

final class NotificationPreferenceRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("NotificationPreferenceRepository returns none for missing preferences") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val preferences = NotificationPreferenceRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(100))
      missing <- preferences.getByUserId(user.id)
    yield assertEquals(missing, None)
  }

  repositoryTest("NotificationPreferenceRepository inserts preferences with upsert") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val preferences = NotificationPreferenceRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(100))
      enabled <- preferences.upsert(samplePreference(101, user.id, isEnabled = true))
      found <- preferences.getByUserId(user.id)
    yield assertEquals(found, Some(enabled))
  }

  repositoryTest("NotificationPreferenceRepository updates existing preferences with upsert") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val preferences = NotificationPreferenceRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(100))
      enabled <- preferences.upsert(samplePreference(101, user.id, isEnabled = true))
      disabled <- preferences.upsert(samplePreference(102, user.id, isEnabled = false))
    yield
      assertEquals(disabled.id, enabled.id)
      assertEquals(disabled.isEnabled, false)
  }
