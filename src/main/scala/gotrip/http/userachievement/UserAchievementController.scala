package gotrip.http.userachievement

import cats.effect.IO
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.userachievement.UserAchievementService
import sttp.tapir.server.ServerEndpoint

final class UserAchievementController(service: UserAchievementService[IO], authSupport: AuthSupport):

  val listMyAchievements: ServerEndpoint[Any, IO] =
    UserAchievementEndpoints.listMyAchievements
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => _ =>
        service.listByUser(authUser.userId).attempt.map {
          case Right(list) => Right(list)
          case Left(error) => Left(HttpError.Internal(error.getMessage))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listMyAchievements)
