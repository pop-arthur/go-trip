package gotrip.http.userachievement

import gotrip.domain.userachievement.UserAchievement
import gotrip.http.{EndpointErrors, HttpError}
import sttp.tapir._
import sttp.tapir.json.circe._

object UserAchievementEndpoints:
  import UserAchievementCodecs.given

  type ErrorResponse = HttpError

  val listMyAchievements: PublicEndpoint[Unit, ErrorResponse, List[UserAchievement], Any] =
    endpoint.get
      .in("users" / "me" / "achievements")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[UserAchievement]])