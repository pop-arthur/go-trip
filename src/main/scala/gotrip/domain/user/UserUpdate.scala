package gotrip.domain.user

final case class UserUpdate(
  email: Option[UserEmail] = None,
  fullName: Option[UserFullName] = None
)