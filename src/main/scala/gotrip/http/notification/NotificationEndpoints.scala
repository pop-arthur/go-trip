package gotrip.http.notification

import gotrip.domain.notification.UserNotification
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.tapir._
import sttp.tapir.json.circe._

object NotificationEndpoints:
  import NotificationCodecs.given

  type ErrorResponse = HttpError

  val listCurrentUserNotifications: Endpoint[String, Unit, ErrorResponse, List[UserNotification], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("notifications")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[UserNotification]])
