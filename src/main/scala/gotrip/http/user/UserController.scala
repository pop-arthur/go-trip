package gotrip.http.user

import cats.effect.IO
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.auth.{AuthSupport, PublicUser}
import gotrip.service.user.UserService
import sttp.tapir.server.ServerEndpoint
import UserCodecs.UserUpdate

final class UserController(
  service: UserService[IO],
  authSupport: AuthSupport
):

  val getCurrentUser: ServerEndpoint[Any, IO] =
    UserEndpoints.getCurrentUser
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => _ =>
        service.findById(authUser.userId).attempt.flatMap {
          case Right(Some(user)) =>
            service.getRoles(user.id).map(roles => Right(PublicUser.from(user, roles)))
          case Right(None) =>
            IO.pure(Left(HttpError.NotFound("User not found")))
          case Left(error) =>
            IO.pure(Left(HttpError.Internal(error.getMessage)))
        }
      }

  val updateCurrentUser: ServerEndpoint[Any, IO] =
    UserEndpoints.updateCurrentUser
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => update =>
        service.findById(authUser.userId).flatMap {
          case Some(user) =>
            val updatedUser = user.copy(
              email = update.email.getOrElse(user.email),
              fullName = update.fullName.getOrElse(user.fullName),
              updatedAt = java.time.Instant.now()
            )
            service.update(updatedUser).attempt.flatMap {
              case Right(n) if n == 1 =>
                service.getRoles(updatedUser.id).map(roles => Right(PublicUser.from(updatedUser, roles)))
              case Right(n) if n == 0 =>
                IO.pure(Left(HttpError.NotFound("User not found")))
              case Right(n) =>
                IO.pure(Left(HttpError.Internal(s"Unexpected update count: $n")))
              case Left(error) =>
                IO.pure(Left(HttpError.Internal(error.getMessage)))
            }
          case None =>
            IO.pure(Left(HttpError.NotFound("User not found")))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getCurrentUser, updateCurrentUser)
