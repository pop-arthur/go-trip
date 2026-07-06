package gotrip.service.auth

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.config.AuthConfig
import gotrip.domain.auth.AuthSession
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.auth.{AuthenticatedUser, LoginRequest, RefreshRequest, RegisterRequest}
import gotrip.repository.auth.AuthSessionRepository
import gotrip.repository.user.UserRepository
import gotrip.service.GeneratedData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.*

final class AuthServiceSpec extends AnyWordSpec with Matchers:

  "AuthService" should {
    "register a new user and create a session" in {
      val users = new RecordingUserRepository
      val sessions = new RecordingAuthSessionRepository
      val service = authService(users, sessions)

      val result = service.register(registerRequest).unsafeRunSync()

      result match
        case Right(response) =>
          response.tokenType shouldBe "Bearer"
          response.user.email shouldBe registerRequest.email
          response.user.fullName shouldBe registerRequest.fullName
          response.user.roles shouldBe List(Role.USER)
          response.accessToken should not be empty
          response.refreshToken should not be empty
          users.created.size shouldBe 1
          users.rolesFor(response.user.id) shouldBe List(Role.USER)
          sessions.sessions.size shouldBe 1
        case Left(error) =>
          fail(s"registration failed: $error")
    }

    "reject registration when email already exists" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      val service = authService(users, new RecordingAuthSessionRepository)

      service.register(registerRequest).unsafeRunSync() shouldBe
        Left(HttpError.Conflict("User with this email already exists"))
    }

    "reject registration with invalid email" in {
      val service = authService(new RecordingUserRepository, new RecordingAuthSessionRepository)

      val request = registerRequest.copy(email = UserEmail("invalid-email"))

      service.register(request).unsafeRunSync() shouldBe Left(HttpError.Validation("Email must be valid"))
    }

    "reject registration with short password" in {
      val service = authService(new RecordingUserRepository, new RecordingAuthSessionRepository)

      val request = registerRequest.copy(password = "short")

      service.register(request).unsafeRunSync() shouldBe
        Left(HttpError.Validation("Password must contain at least 8 characters"))
    }

    "login an existing user and create a session" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      users.setRoles(existingUser.id, List(Role.ADMIN, Role.USER, Role.USER))
      val sessions = new RecordingAuthSessionRepository
      val service = authService(users, sessions)

      val result = service.login(LoginRequest(existingUser.email, password)).unsafeRunSync()

      result match
        case Right(response) =>
          response.user.id shouldBe existingUser.id
          response.user.roles shouldBe List(Role.ADMIN, Role.USER)
          sessions.sessions.size shouldBe 1
        case Left(error) =>
          fail(s"login failed: $error")
    }

    "reject login for unknown email" in {
      val service = authService(new RecordingUserRepository, new RecordingAuthSessionRepository)

      service.login(LoginRequest(existingUser.email, password)).unsafeRunSync() shouldBe
        Left(HttpError.Unauthorized("Invalid email or password"))
    }

    "reject login for wrong password" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      val service = authService(users, new RecordingAuthSessionRepository)

      service.login(LoginRequest(existingUser.email, "wrongPassword123")).unsafeRunSync() shouldBe
        Left(HttpError.Unauthorized("Invalid email or password"))
    }

    "refresh an active session and rotate the refresh token hash" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      val sessions = new RecordingAuthSessionRepository
      val service = authService(users, sessions)

      val login = service.login(LoginRequest(existingUser.email, password)).unsafeRunSync().toOption.get
      val authUser = jwtService.validateRefresh(login.refreshToken).unsafeRunSync().toOption.get
      val oldHash = sessions.sessions(authUser.sessionId).refreshTokenHash

      val refreshed = service.refresh(RefreshRequest(login.refreshToken)).unsafeRunSync()

      refreshed match
        case Right(response) =>
          response.refreshToken should not be login.refreshToken
          sessions.sessions(authUser.sessionId).refreshTokenHash should not be oldHash
          jwtService.validateRefresh(response.refreshToken).unsafeRunSync().map(_.sessionId) shouldBe Right(authUser.sessionId)
        case Left(error) =>
          fail(s"refresh failed: $error")
    }

    "reject refresh when active session is missing" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      val sessions = new RecordingAuthSessionRepository
      val service = authService(users, sessions)
      val login = service.login(LoginRequest(existingUser.email, password)).unsafeRunSync().toOption.get
      val authUser = jwtService.validateRefresh(login.refreshToken).unsafeRunSync().toOption.get
      sessions.sessions.remove(authUser.sessionId)

      service.refresh(RefreshRequest(login.refreshToken)).unsafeRunSync() shouldBe
        Left(HttpError.Unauthorized("Invalid email or password"))
    }

    "reject refresh when token hash does not match the active session" in {
      val users = new RecordingUserRepository
      users.seed(existingUser)
      val sessions = new RecordingAuthSessionRepository
      val service = authService(users, sessions)
      val login = service.login(LoginRequest(existingUser.email, password)).unsafeRunSync().toOption.get
      val authUser = jwtService.validateRefresh(login.refreshToken).unsafeRunSync().toOption.get
      val activeSession = sessions.sessions(authUser.sessionId)
      sessions.sessions.update(authUser.sessionId, activeSession.copy(refreshTokenHash = "different"))

      service.refresh(RefreshRequest(login.refreshToken)).unsafeRunSync() shouldBe
        Left(HttpError.Unauthorized("Invalid email or password"))
    }

    "logout revokes the session" in {
      val sessions = new RecordingAuthSessionRepository
      val service = authService(new RecordingUserRepository, sessions)
      val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000099")
      sessions.sessions.update(sessionId, activeSession(sessionId))

      service.logout(authenticatedUser(sessionId)).unsafeRunSync() shouldBe Right(())
      sessions.sessions(sessionId).revokedAt should not be empty
    }
  }

  private val password = "strongPassword123"
  private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
  private val now = Instant.parse("2026-07-06T10:00:00Z")

  private val authConfig = AuthConfig(
    issuer = "gotrip",
    jwtSecret = "test-secret",
    accessTokenTtl = 15.minutes,
    refreshTokenTtl = 30.days,
    passwordCost = 4
  )

  private val jwtService = new JwtService[IO](authConfig)

  private val registerRequest = RegisterRequest(
    email = UserEmail("traveler@example.com"),
    password = password,
    fullName = UserFullName(Some("Test Traveler"))
  )

  private val existingUser = User(
    id = userId,
    email = registerRequest.email,
    passwordHash = UserPasswordHash(s"hashed:$password"),
    fullName = registerRequest.fullName,
    createdAt = now,
    updatedAt = now
  )

  private def authService(
    users: RecordingUserRepository,
    sessions: RecordingAuthSessionRepository
  ): AuthService[IO] =
    given GeneratedData[IO] = StaticGeneratedData
    AuthService[IO](
      users,
      sessions,
      StaticPasswordHasher,
      jwtService,
      authConfig.refreshTokenTtl
    )

  private def authenticatedUser(sessionId: UUID): AuthenticatedUser =
    AuthenticatedUser(existingUser.id, existingUser.email, List(Role.USER), sessionId)

  private def activeSession(id: UUID): AuthSession =
    AuthSession(
      id = id,
      userId = existingUser.id,
      refreshTokenHash = "hash",
      expiresAt = Instant.now().plusSeconds(3600),
      revokedAt = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

private object StaticGeneratedData extends GeneratedData[IO]:
  override def newId(): IO[UUID] =
    IO.pure(UUID.fromString("00000000-0000-0000-0000-000000000001"))

  override def now(): IO[Instant] =
    IO.pure(Instant.parse("2026-07-06T10:00:00Z"))

private object StaticPasswordHasher extends PasswordHasher[IO]:
  override def hash(password: String): IO[String] =
    IO.pure(s"hashed:$password")

  override def verify(password: String, hash: String): IO[Boolean] =
    IO.pure(hash == s"hashed:$password")

private final class RecordingUserRepository extends UserRepository[IO]:
  private val users = mutable.Map.empty[UserId, User]
  private val roles = mutable.Map.empty[UserId, List[Role]]
  var created: List[User] = Nil

  def seed(user: User): Unit =
    users.update(user.id, user)
    roles.update(user.id, List(Role.USER))

  def setRoles(userId: UserId, userRoles: List[Role]): Unit =
    roles.update(userId, userRoles)

  def rolesFor(userId: UserId): List[Role] =
    roles.getOrElse(userId, Nil)

  override def create(user: User): IO[User] =
    users.update(user.id, user)
    created = user :: created
    IO.pure(user)

  override def findByEmail(email: UserEmail): IO[Option[User]] =
    IO.pure(users.values.find(_.email == email))

  override def findById(id: UserId): IO[Option[User]] =
    IO.pure(users.get(id))

  override def update(user: User): IO[Int] =
    val existed = users.contains(user.id)
    users.update(user.id, user)
    IO.pure(if existed then 1 else 0)

  override def delete(id: UserId): IO[Int] =
    IO.pure(users.remove(id).fold(0)(_ => 1))

  override def addRole(userId: UserId, role: Role, createdAt: Instant, updatedAt: Instant): IO[Int] =
    val updated = (roles.getOrElse(userId, Nil) :+ role).distinct
    roles.update(userId, updated)
    IO.pure(1)

  override def removeRole(userId: UserId, role: Role): IO[Int] =
    roles.update(userId, roles.getOrElse(userId, Nil).filterNot(_ == role))
    IO.pure(1)

  override def getRoles(userId: UserId): IO[List[Role]] =
    IO.pure(roles.getOrElse(userId, Nil))

private final class RecordingAuthSessionRepository extends AuthSessionRepository[IO]:
  val sessions: mutable.Map[UUID, AuthSession] = mutable.Map.empty

  override def create(session: AuthSession): IO[AuthSession] =
    sessions.update(session.id, session)
    IO.pure(session)

  override def findActive(id: UUID, now: Instant): IO[Option[AuthSession]] =
    IO.pure(sessions.get(id).filter(session => session.revokedAt.isEmpty && session.expiresAt.isAfter(now)))

  override def rotate(id: UUID, refreshTokenHash: String, expiresAt: Instant, now: Instant): IO[Int] =
    sessions.get(id) match
      case Some(session) if session.revokedAt.isEmpty =>
        sessions.update(id, session.copy(refreshTokenHash = refreshTokenHash, expiresAt = expiresAt, updatedAt = now))
        IO.pure(1)
      case _ =>
        IO.pure(0)

  override def revoke(id: UUID, now: Instant): IO[Int] =
    sessions.get(id) match
      case Some(session) if session.revokedAt.isEmpty =>
        sessions.update(id, session.copy(revokedAt = Some(now), updatedAt = now))
        IO.pure(1)
      case _ =>
        IO.pure(0)
