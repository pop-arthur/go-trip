package gotrip.http.notificationpreference

import gotrip.domain.notificationpreference.NotificationPreference
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.tapir._
import sttp.tapir.json.circe._
import NotificationPreferenceCodecs.{NotificationPreferenceUpdate, given}

object NotificationPreferenceEndpoints:
  import NotificationPreferenceCodecs.given

  type ErrorResponse = HttpError

  val getPreference: Endpoint[String, Unit, ErrorResponse, NotificationPreference, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("notification-preferences")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[NotificationPreference])

  val updatePreference: Endpoint[String, NotificationPreferenceUpdate, ErrorResponse, NotificationPreference, Any] =
    endpoint.put
      .securityIn(AuthEndpoints.bearer)
      .in("notification-preferences")
      .in(jsonBody[NotificationPreferenceUpdate])
      .errorOut(EndpointErrors.validation)
      .out(jsonBody[NotificationPreference])
