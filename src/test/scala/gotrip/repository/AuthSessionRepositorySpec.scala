package gotrip.repository

import cats.effect.IO
import gotrip.repository.auth.AuthSessionRepository
import gotrip.repository.user.UserRepository

final class AuthSessionRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("AuthSessionRepository creates and finds active sessions") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val sessions = AuthSessionRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(10))
      active = sampleSession(11, user.id, expiresAt = t(100), revokedAt = None)
      created <- sessions.create(active)
      found <- sessions.findActive(active.id, t(20))
    yield
      assertEquals(created, active)
      assertEquals(found, Some(active))
  }

  repositoryTest("AuthSessionRepository ignores expired sessions") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val sessions = AuthSessionRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(12))
      expired = sampleSession(13, user.id, expiresAt = t(5), revokedAt = None)
      _ <- sessions.create(expired)
      found <- sessions.findActive(expired.id, t(20))
    yield assertEquals(found, None)
  }

  repositoryTest("AuthSessionRepository rotates active sessions") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val sessions = AuthSessionRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(14))
      active = sampleSession(15, user.id, expiresAt = t(100), revokedAt = None)
      _ <- sessions.create(active)
      rows <- sessions.rotate(active.id, "rotated-hash", t(120), t(30))
      found <- sessions.findActive(active.id, t(40))
    yield
      assertEquals(rows, 1)
      assertEquals(found.map(_.refreshTokenHash), Some("rotated-hash"))
  }

  repositoryTest("AuthSessionRepository revokes active sessions") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val sessions = AuthSessionRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(16))
      active = sampleSession(17, user.id, expiresAt = t(100), revokedAt = None)
      _ <- sessions.create(active)
      rows <- sessions.revoke(active.id, t(50))
      found <- sessions.findActive(active.id, t(60))
    yield
      assertEquals(rows, 1)
      assertEquals(found, None)
  }
