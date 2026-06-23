package gotrip.http.achievement

import cats.effect.IO
import gotrip.domain.achievement._
import gotrip.http.HttpError
import gotrip.service.achievement.AchievementService
import sttp.tapir.server.ServerEndpoint
import AchievementCodecs.{AchievementCreateRequest, AchievementUpdateRequest}

final class AdminAchievementController(service: AchievementService[IO]):

  val adminCreate: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminCreateAchievement.serverLogic { request =>
      val achievement = Achievement(
        id = AchievementId(0L),
        code = AchievementCode(request.code),
        title = AchievementTitle(request.title),
        description = AchievementDescription(request.description),
        conditionType = request.conditionType,
        conditionValue = request.conditionValue,
        iconUrl = AchievementIconUrl(request.iconUrl),
        createdAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now()
      )
      service.create(achievement).attempt.map {
        case Right(created) => Right(created)
        case Left(error)    => Left(HttpError.Internal(error.getMessage))
      }
    }

  val adminUpdate: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminUpdateAchievement.serverLogic { case (id, update) =>
      service.findById(id).flatMap {
        case Some(existing) =>
          val updated = existing.copy(
            code = update.code.map(AchievementCode.apply).getOrElse(existing.code),
            title = update.title.map(AchievementTitle.apply).getOrElse(existing.title),
            description = update.description.map(s => AchievementDescription(Some(s))).getOrElse(existing.description),
            conditionType = update.conditionType.getOrElse(existing.conditionType),
            conditionValue = update.conditionValue.getOrElse(existing.conditionValue),
            iconUrl = update.iconUrl.map(s => AchievementIconUrl(Some(s))).getOrElse(existing.iconUrl),
            updatedAt = java.time.Instant.now()
          )
          service.update(updated).attempt.map {
            case Right(n) if n == 1 => Right(updated)
            case Right(n) if n == 0 => Left(HttpError.NotFound(s"Achievement ${id.value} not found"))
            case Right(n)           => Left(HttpError.Internal(s"Unexpected update count: $n"))
            case Left(error)        => Left(HttpError.Internal(error.getMessage))
          }
        case None =>
          IO.pure(Left(HttpError.NotFound(s"Achievement ${id.value} not found")))
      }
    }

  val adminDelete: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminDeleteAchievement.serverLogic { id =>
      service.delete(id).attempt.map {
        case Right(n) if n == 1 => Right(())
        case Right(n) if n == 0 => Left(HttpError.NotFound(s"Achievement ${id.value} not found"))
        case Right(n)           => Left(HttpError.Internal(s"Unexpected delete count: $n"))
        case Left(error)        => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(adminCreate, adminUpdate, adminDelete)