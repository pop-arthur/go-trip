package gotrip.http.userachievement

import cats.effect.IO
import gotrip.domain.user.UserId
import gotrip.http.HttpError
import gotrip.service.userachievement.UserAchievementService
import sttp.tapir.server.ServerEndpoint

final class UserAchievementController(service: UserAchievementService[IO]):

  val listMyAchievements: ServerEndpoint[Any, IO] =
    UserAchievementEndpoints.listMyAchievements.serverLogic { _ =>
      val userId = UserId(1L)
      service.listByUser(userId).attempt.map {
        case Right(list) => Right(list)
        case Left(error) => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listMyAchievements)