package gotrip.service.auth

import cats.effect.Clock
import cats.effect.kernel.Sync
import cats.syntax.all._
import gotrip.domain.auth.AuthSession
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.auth.{AuthenticatedUser, AuthResponse, LoginRequest, PublicUser, RefreshRequest, RegisterRequest}
import gotrip.repository.auth.AuthSessionRepository
import gotrip.repository.user.UserRepository
import gotrip.service.GeneratedData

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

final class AuthService[F[_]: Sync: Clock: GeneratedData](
  userRepository: UserRepository[F],
  sessionRepository: AuthSessionRepository[F],
  passwordHasher: PasswordHasher[F],
  jwtService: JwtService[F],
  refreshTokenTtl: FiniteDuration
):

  def register(request: RegisterRequest): F[Either[HttpError, AuthResponse]] =
    validateCredentials(request.email, request.password).fold(
      error => error.asLeft[AuthResponse].pure[F],
      _ =>
        userRepository.findByEmail(request.email).flatMap {
          case Some(_) =>
            HttpError.Conflict("User with this email already exists").asLeft[AuthResponse].pure[F]
          case None =>
            for {
              passwordHash <- passwordHasher.hash(request.password)
              user <- newUser(request.email, UserPasswordHash(passwordHash), request.fullName)
              created <- userRepository.create(user)
              _ <- userRepository.addRole(user.id, Role.USER, user.createdAt, user.updatedAt)
              response <- issueAuthResponse(created, List(Role.USER))
            } yield response.asRight[HttpError]
        }
    )

  def login(request: LoginRequest): F[Either[HttpError, AuthResponse]] =
    userRepository.findByEmail(request.email).flatMap {
      case None =>
        unauthorized.asLeft[AuthResponse].pure[F]
      case Some(user) =>
        passwordHasher.verify(request.password, user.passwordHash.value).flatMap {
          case false =>
            unauthorized.asLeft[AuthResponse].pure[F]
          case true =>
            userRepository.getRoles(user.id).flatMap { roles =>
              issueAuthResponse(user, normalizeRoles(roles)).map(_.asRight[HttpError])
            }
        }
    }

  def refresh(request: RefreshRequest): F[Either[HttpError, AuthResponse]] =
    jwtService.validateRefresh(request.refreshToken).flatMap {
      case Left(error) =>
        error.asLeft[AuthResponse].pure[F]
      case Right(authUser) =>
        for
          now <- nowInstant
          session <- sessionRepository.findActive(authUser.sessionId, now)
          result <- session match
            case None =>
              unauthorized.asLeft[AuthResponse].pure[F]
            case Some(activeSession) if activeSession.refreshTokenHash != TokenHashing.sha256(request.refreshToken) =>
              unauthorized.asLeft[AuthResponse].pure[F]
            case Some(_) =>
              userRepository.findById(authUser.userId).flatMap {
                case None =>
                  unauthorized.asLeft[AuthResponse].pure[F]
                case Some(user) =>
                  userRepository.getRoles(user.id).flatMap { roles =>
                    rotateAndIssue(user, normalizeRoles(roles), authUser.sessionId, now).map(_.asRight[HttpError])
                  }
              }
        yield result
    }

  def logout(authUser: AuthenticatedUser): F[Either[HttpError, Unit]] =
    nowInstant.flatMap { now =>
      sessionRepository.revoke(authUser.sessionId, now).map(_ => ().asRight[HttpError])
    }

  private def issueAuthResponse(user: User, roles: List[Role]): F[AuthResponse] =
    for
      now <- nowInstant
      sessionId <- GeneratedData[F].newId()
      accessToken <- jwtService.issueAccessToken(user.id, user.email, roles, sessionId, now)
      refreshToken <- jwtService.issueRefreshToken(user.id, user.email, roles, sessionId, now)
      _ <- sessionRepository.create(
        AuthSession(
          id = sessionId,
          userId = user.id,
          refreshTokenHash = TokenHashing.sha256(refreshToken),
          expiresAt = now.plusSeconds(refreshTokenTtl.toSeconds),
          revokedAt = None,
          createdAt = now,
          updatedAt = now
        )
      )
    yield AuthResponse(accessToken, refreshToken, "Bearer", PublicUser.from(user, roles))

  private def rotateAndIssue(user: User, roles: List[Role], sessionId: UUID, now: Instant): F[AuthResponse] =
    for
      accessToken <- jwtService.issueAccessToken(user.id, user.email, roles, sessionId, now)
      refreshToken <- jwtService.issueRefreshToken(user.id, user.email, roles, sessionId, now)
      _ <- sessionRepository.rotate(
        id = sessionId,
        refreshTokenHash = TokenHashing.sha256(refreshToken),
        expiresAt = now.plusSeconds(refreshTokenTtl.toSeconds),
        now = now
      )
    yield AuthResponse(accessToken, refreshToken, "Bearer", PublicUser.from(user, roles))

  private def validateCredentials(email: UserEmail, password: String): Either[HttpError, Unit] =
    if !email.value.contains("@") then Left(HttpError.Validation("Email must be valid"))
    else if password.length < 8 then Left(HttpError.Validation("Password must contain at least 8 characters"))
    else Right(())

  private def normalizeRoles(roles: List[Role]): List[Role] =
    if roles.isEmpty then List(Role.USER) else roles.distinct

  private def nowInstant: F[Instant] =
    GeneratedData[F].now()

  private def newUser(email: UserEmail, passwordHash: UserPasswordHash, fullName: UserFullName): F[User] =
    for
      id <- GeneratedData[F].newId()
      now <- nowInstant
    yield User(UserId(id), email, passwordHash, fullName, now, now)

  private val unauthorized: HttpError =
    HttpError.Unauthorized("Invalid email or password")
