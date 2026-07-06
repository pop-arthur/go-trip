package gotrip.http.userachievement

import gotrip.domain.userachievement.UserAchievement
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.tapir._
import sttp.tapir.json.circe._

object UserAchievementEndpoints:
  import UserAchievementCodecs.given

  type ErrorResponse = HttpError

  val listMyAchievements: Endpoint[String, Unit, ErrorResponse, List[UserAchievement], Any] =
    endpoint.get
      .tag(SwaggerTags.UserAchievements)
      .securityIn(AuthEndpoints.bearer)
      .in("users" / "me" / "achievements")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[UserAchievement]])
