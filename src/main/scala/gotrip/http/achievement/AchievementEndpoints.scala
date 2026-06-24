package gotrip.http.achievement

import gotrip.domain.achievement.{Achievement, AchievementId}
import gotrip.http.{EndpointErrors, HttpError}
import sttp.tapir._
import sttp.tapir.json.circe._

object AchievementEndpoints:
  import AchievementCodecs.given

  type ErrorResponse = HttpError

  val listAchievements: PublicEndpoint[Unit, ErrorResponse, List[Achievement], Any] =
    endpoint.get
      .in("achievements")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Achievement]])