package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.user.UserRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class NotificationPreferenceRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "NotificationPreferenceRepository" should {
    "get missing preferences and upsert insert/update by user" in {
      val users = UserRepository.makePostgres[IO](sessionPool)
      val preferences = NotificationPreferenceRepository.makePostgres[IO](sessionPool)
      val user = users.create(sampleUser(100)).unsafeRunSync()

      preferences.getByUserId(user.id).unsafeRunSync() shouldBe None
      val enabled = preferences.upsert(samplePreference(101, user.id, isEnabled = true)).unsafeRunSync()
      preferences.getByUserId(user.id).unsafeRunSync() shouldBe Some(enabled)
      val disabled = preferences.upsert(samplePreference(102, user.id, isEnabled = false)).unsafeRunSync()
      disabled.id shouldBe enabled.id
      disabled.isEnabled shouldBe false
    }
  }
