package gotrip.http.user

import cats.effect.IO
import gotrip.domain.user._
import gotrip.http.{HttpError}
import gotrip.http.auth.{AuthSupport, PublicUser}
import gotrip.service.user.UserService
import sttp.tapir.server.ServerEndpoint

import java.time.Instant

final class UserController(
  service: UserService[IO],
  authSupport: AuthSupport
):

  val getCurrentUser: ServerEndpoint[Any, IO] =
    UserEndpoints.getCurrentUser
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => _ =>
        service.findById(authUser.userId).attempt.map {
          case Right(Some(user)) =>
            service.getRoles(authUser.userId).map { roles =>
              Right(PublicUser.from(user, roles))
            }
          case Right(None)       => IO.pure(Left(HttpError.NotFound("User not found")))
          case Left(error)       => IO.pure(Left(HttpError.Internal(error.getMessage)))
        }.flatten
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
              updatedAt = Instant.now()
            )
            service.update(updatedUser).flatMap {
              case 1 =>
                service.getRoles(authUser.userId).map { roles =>
                  Right(PublicUser.from(updatedUser, roles))
                }
              case 0 =>
                IO.pure(Left(HttpError.NotFound("User not found")))
            }.attempt.map {
              case Right(Right(pu)) => Right(pu)
              case Right(Left(err)) => Left(err)
              case Left(error)      => Left(HttpError.Internal(error.getMessage))
            }
          case None =>
            IO.pure(Left(HttpError.NotFound("User not found")))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getCurrentUser, updateCurrentUser)