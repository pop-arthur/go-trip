package gotrip.http.achievement

import cats.effect.IO
import gotrip.domain.achievement._
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.achievement.AchievementService
import sttp.tapir.server.ServerEndpoint
import AchievementCodecs.{AchievementCreateRequest, AchievementUpdateRequest}
import java.time.Instant
import java.util.UUID

final class AdminAchievementController(service: AchievementService[IO], authSupport: AuthSupport):
  private val placeholderId = AchievementId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
  private val placeholderTime = Instant.EPOCH

  val adminCreate: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminCreateAchievement
      .serverSecurityLogic(token => authSupport.authenticate(token).map(_.flatMap(user => authSupport.requireRole(user, Role.ADMIN))))
      .serverLogic { _ => request =>
      val achievement = Achievement(
        id = placeholderId,
        code = AchievementCode(request.code),
        title = AchievementTitle(request.title),
        description = AchievementDescription(request.description),
        conditionType = request.conditionType,
        conditionValue = request.conditionValue,
        iconUrl = AchievementIconUrl(request.iconUrl),
        createdAt = placeholderTime,
        updatedAt = placeholderTime
      )
      service.create(achievement).attempt.map {
        case Right(created) => Right(created)
        case Left(error)    => Left(HttpError.Internal(error.getMessage))
      }
    }

  val adminUpdate: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminUpdateAchievement
      .serverSecurityLogic(token => authSupport.authenticate(token).map(_.flatMap(user => authSupport.requireRole(user, Role.ADMIN))))
      .serverLogic { _ => { case (id, update) =>
      service.findById(id).flatMap {
        case Some(existing) =>
          val updated = existing.copy(
            code = update.code.map(AchievementCode.apply).getOrElse(existing.code),
            title = update.title.map(AchievementTitle.apply).getOrElse(existing.title),
            description = update.description.map(s => AchievementDescription(Some(s))).getOrElse(existing.description),
            conditionType = update.conditionType.getOrElse(existing.conditionType),
            conditionValue = update.conditionValue.getOrElse(existing.conditionValue),
            iconUrl = update.iconUrl.map(s => AchievementIconUrl(Some(s))).getOrElse(existing.iconUrl)
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
    }}

  val adminDelete: ServerEndpoint[Any, IO] =
    AdminAchievementEndpoints.adminDeleteAchievement
      .serverSecurityLogic(token => authSupport.authenticate(token).map(_.flatMap(user => authSupport.requireRole(user, Role.ADMIN))))
      .serverLogic { _ => id =>
      service.delete(id).attempt.map {
        case Right(n) if n == 1 => Right(())
        case Right(n) if n == 0 => Left(HttpError.NotFound(s"Achievement ${id.value} not found"))
        case Right(n)           => Left(HttpError.Internal(s"Unexpected delete count: $n"))
        case Left(error)        => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(adminCreate, adminUpdate, adminDelete)
