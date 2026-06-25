package gotrip.http.auth

import cats.effect.IO
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.service.auth.JwtService

final class AuthSupport(jwtService: JwtService[IO]):
  def authenticate(token: String): IO[Either[HttpError, AuthenticatedUser]] =
    jwtService.authenticateAccess(token)

  def requireRole(user: AuthenticatedUser, role: Role): Either[HttpError, AuthenticatedUser] =
    Either.cond(
      user.roles.contains(role),
      user,
      HttpError.Forbidden("You do not have permission to access this resource")
    )
