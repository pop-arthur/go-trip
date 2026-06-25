package gotrip.http.auth

import gotrip.domain.user.*
import gotrip.domain.userrole.Role

import java.time.Instant
import java.util.UUID

final case class AuthenticatedUser(
  userId: UserId,
  email: UserEmail,
  roles: List[Role],
  sessionId: UUID
)

final case class PublicUser(
  id: UserId,
  email: UserEmail,
  fullName: UserFullName,
  roles: List[Role],
  createdAt: Instant,
  updatedAt: Instant
)

object PublicUser:
  def from(user: User, roles: List[Role]): PublicUser =
    PublicUser(
      id = user.id,
      email = user.email,
      fullName = user.fullName,
      roles = roles,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )

final case class RegisterRequest(
  email: UserEmail,
  password: String,
  fullName: UserFullName
)

final case class LoginRequest(
  email: UserEmail,
  password: String
)

final case class RefreshRequest(
  refreshToken: String
)

final case class AuthResponse(
  accessToken: String,
  refreshToken: String,
  tokenType: String,
  user: PublicUser
)
