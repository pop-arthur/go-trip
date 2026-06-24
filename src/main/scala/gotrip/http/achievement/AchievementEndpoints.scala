package gotrip.http.achievement

import gotrip.domain.achievement.{Achievement, AchievementId}
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.tapir._
import sttp.tapir.json.circe._

object AchievementEndpoints:
  import AchievementCodecs.given

  type ErrorResponse = HttpError

  val listAchievements: Endpoint[String, Unit, ErrorResponse, List[Achievement], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("achievements")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Achievement]])
