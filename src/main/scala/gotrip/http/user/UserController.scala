package gotrip.http.user

import cats.effect.IO
import gotrip.domain.user._
import gotrip.http.HttpError
import gotrip.service.user.UserService
import sttp.tapir.server.ServerEndpoint
import UserCodecs.UserUpdate

final class UserController(
  service: UserService[IO]
):

  val getCurrentUser: ServerEndpoint[Any, IO] =
    UserEndpoints.getCurrentUser.serverLogic { _ =>
      val userId = UserId(1L)
      service.findById(userId).attempt.map {
        case Right(Some(user)) => Right(user)
        case Right(None)       => Left(HttpError.NotFound("User not found"))
        case Left(error)       => Left(HttpError.Internal(error.getMessage))
      }
    }

  val updateCurrentUser: ServerEndpoint[Any, IO] =
    UserEndpoints.updateCurrentUser.serverLogic { update =>
      val userId = UserId(1L)
      service.findById(userId).flatMap {
        case Some(user) =>
          val updatedUser = user.copy(
            email = update.email.getOrElse(user.email),
            fullName = update.fullName.getOrElse(user.fullName),
            updatedAt = java.time.Instant.now()
          )
          service.update(updatedUser).attempt.map {
            case Right(n) if n == 1 => Right(updatedUser)
            case Right(n) if n == 0 => Left(HttpError.NotFound("User not found"))
            case Right(n)           => Left(HttpError.Internal(s"Unexpected update count: $n"))
            case Left(error)        => Left(HttpError.Internal(error.getMessage))
          }
        case None =>
          IO.pure(Left(HttpError.NotFound("User not found")))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getCurrentUser, updateCurrentUser)