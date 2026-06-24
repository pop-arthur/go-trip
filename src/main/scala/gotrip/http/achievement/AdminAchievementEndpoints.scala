package gotrip.http.achievement

import gotrip.domain.achievement.{Achievement, AchievementId}
import gotrip.http.{EndpointErrors, HttpError}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import AchievementCodecs.{AchievementCreateRequest, AchievementUpdateRequest, given}

object AdminAchievementEndpoints {
  import AchievementCodecs.given

  type ErrorResponse = HttpError

  val adminCreateAchievement: PublicEndpoint[AchievementCreateRequest, ErrorResponse, Achievement, Any] =
    endpoint.post
      .in("admin" / "achievements")
      .in(jsonBody[AchievementCreateRequest])
      .errorOut(EndpointErrors.validationOrConflict)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Achievement])

  val adminUpdateAchievement: PublicEndpoint[(AchievementId, AchievementUpdateRequest), ErrorResponse, Achievement, Any] =
    endpoint.patch
      .in("admin" / "achievements" / path[AchievementId]("achievementId"))
      .in(jsonBody[AchievementUpdateRequest])
      .errorOut(EndpointErrors.validationOrNotFoundOrConflict)
      .out(jsonBody[Achievement])

  val adminDeleteAchievement: PublicEndpoint[AchievementId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("admin" / "achievements" / path[AchievementId]("achievementId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
}