package gotrip.http.notificationpreference

import gotrip.domain.notificationpreference.NotificationPreference
import gotrip.http.{EndpointErrors, HttpError}
import sttp.tapir._
import sttp.tapir.json.circe._
import NotificationPreferenceCodecs.{NotificationPreferenceUpdate, given}

object NotificationPreferenceEndpoints:
  import NotificationPreferenceCodecs.given

  type ErrorResponse = HttpError

  val getPreference: PublicEndpoint[Unit, ErrorResponse, NotificationPreference, Any] =
    endpoint.get
      .in("notification-preferences")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[NotificationPreference])

  val updatePreference: PublicEndpoint[NotificationPreferenceUpdate, ErrorResponse, NotificationPreference, Any] =
    endpoint.put
      .in("notification-preferences")
      .in(jsonBody[NotificationPreferenceUpdate])
      .errorOut(EndpointErrors.validation)
      .out(jsonBody[NotificationPreference])