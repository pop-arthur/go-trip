package gotrip.service.auth

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.config.AuthConfig
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

final class AuthCryptoSpec extends AnyWordSpec with Matchers:

  "PasswordHasher" should {
    "hash passwords without storing the plain value and verify matches" in {
      val hasher = PasswordHasher.bcrypt[IO](4)
      val hash = hasher.hash("strongPassword123").unsafeRunSync()

      hash should not be "strongPassword123"
      hasher.verify("strongPassword123", hash).unsafeRunSync() shouldBe true
      hasher.verify("wrongPassword123", hash).unsafeRunSync() shouldBe false
    }
  }

  "JwtService" should {
    "validate access tokens and reject refresh tokens as access tokens" in {
      val service = new JwtService[IO](authConfig)
      val now = Instant.parse("2026-06-24T10:00:00Z")
      val sessionId = UUID.randomUUID()

      val accessToken = service
        .issueAccessToken(UserId(1L), UserEmail("traveler@example.com"), List(Role.USER), sessionId, now)
        .unsafeRunSync()
      val refreshToken = service
        .issueRefreshToken(UserId(1L), UserEmail("traveler@example.com"), List(Role.USER), sessionId, now)
        .unsafeRunSync()

      service.authenticateAccess(accessToken).unsafeRunSync().map(_.userId) shouldBe Right(UserId(1L))
      service.authenticateAccess(refreshToken).unsafeRunSync() match
        case Left(error) => error.code shouldBe "UNAUTHORIZED"
        case Right(_)    => fail("refresh token was accepted as an access token")
    }
  }

  private val authConfig = AuthConfig(
    issuer = "gotrip",
    jwtSecret = "test-secret",
    accessTokenTtl = 15.minutes,
    refreshTokenTtl = 30.days,
    passwordCost = 4
  )
