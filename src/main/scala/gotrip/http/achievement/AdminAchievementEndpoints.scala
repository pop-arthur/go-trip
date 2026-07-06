package gotrip.http.achievement

import gotrip.domain.achievement.{Achievement, AchievementId}
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import AchievementCodecs.{AchievementCreateRequest, AchievementUpdateRequest, given}

object AdminAchievementEndpoints {
  import AchievementCodecs.given

  type ErrorResponse = HttpError

  val adminCreateAchievement: Endpoint[String, AchievementCreateRequest, ErrorResponse, Achievement, Any] =
    endpoint.post
      .tag(SwaggerTags.AdminAchievements)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "achievements")
      .in(jsonBody[AchievementCreateRequest])
      .errorOut(EndpointErrors.validationOrConflict)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Achievement])

  val adminUpdateAchievement: Endpoint[String, (AchievementId, AchievementUpdateRequest), ErrorResponse, Achievement, Any] =
    endpoint.patch
      .tag(SwaggerTags.AdminAchievements)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "achievements" / path[AchievementId]("achievementId"))
      .in(jsonBody[AchievementUpdateRequest])
      .errorOut(EndpointErrors.validationOrNotFoundOrConflict)
      .out(jsonBody[Achievement])

  val adminDeleteAchievement: Endpoint[String, AchievementId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.AdminAchievements)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "achievements" / path[AchievementId]("achievementId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
}
