package gotrip.domain.userrole

import java.time.Instant

import gotrip.domain.user.{UserId}
import gotrip.domain.userrole._

final case class UserRole(
  id: UserRoleId,
  userId: UserId,
  role: Role,
  createdAt: Instant,
  updatedAt: Instant
)