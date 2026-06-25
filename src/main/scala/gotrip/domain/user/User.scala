package gotrip.domain.user

import java.time.Instant

final case class User(
  id: UserId,
  email: UserEmail,
  passwordHash: UserPasswordHash,
  fullName: UserFullName,
  createdAt: Instant,
  updatedAt: Instant
)