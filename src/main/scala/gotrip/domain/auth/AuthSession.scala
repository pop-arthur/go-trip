package gotrip.domain.auth

import gotrip.domain.user.UserId

import java.time.Instant
import java.util.UUID

final case class AuthSession(
  id: UUID,
  userId: UserId,
  refreshTokenHash: String,
  expiresAt: Instant,
  revokedAt: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant
)
