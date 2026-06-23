package gotrip.http.achievement

import cats.effect.IO
import gotrip.http.HttpError
import gotrip.service.achievement.AchievementService
import sttp.tapir.server.ServerEndpoint

final class AchievementController(service: AchievementService[IO]):

  val listAchievements: ServerEndpoint[Any, IO] =
    AchievementEndpoints.listAchievements.serverLogic { _ =>
      service.listAll().attempt.map {
        case Right(list) => Right(list)
        case Left(error) => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listAchievements)