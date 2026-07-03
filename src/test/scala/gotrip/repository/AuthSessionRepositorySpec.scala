package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.repository.auth.AuthSessionRepository
import gotrip.repository.user.UserRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class AuthSessionRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "AuthSessionRepository" should {
    "create, find active, rotate, revoke, and ignore expired sessions" in {
      val users = UserRepository.makePostgres[IO](sessionPool)
      val sessions = AuthSessionRepository.makePostgres[IO](sessionPool)
      val user = users.create(sampleUser(10)).unsafeRunSync()
      val active = sampleSession(11, user.id, expiresAt = t(100), revokedAt = None)
      val expired = sampleSession(12, user.id, expiresAt = t(5), revokedAt = None)

      sessions.create(active).unsafeRunSync() shouldBe active
      sessions.create(expired).unsafeRunSync() shouldBe expired
      sessions.findActive(active.id, t(20)).unsafeRunSync() shouldBe Some(active)
      sessions.findActive(expired.id, t(20)).unsafeRunSync() shouldBe None

      sessions.rotate(active.id, "rotated-hash", t(120), t(30)).unsafeRunSync() shouldBe 1
      sessions.findActive(active.id, t(40)).unsafeRunSync().map(_.refreshTokenHash) shouldBe Some("rotated-hash")
      sessions.revoke(active.id, t(50)).unsafeRunSync() shouldBe 1
      sessions.findActive(active.id, t(60)).unsafeRunSync() shouldBe None
    }
  }
