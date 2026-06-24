package gotrip.http.user

import gotrip.domain.user.{User, UserId}
import gotrip.http.{EndpointErrors, HttpError}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import UserCodecs.{UserUpdate, given}

object UserEndpoints:
  import UserCodecs.given

  type ErrorResponse = HttpError

  val getCurrentUser: PublicEndpoint[Unit, ErrorResponse, User, Any] =
    endpoint.get
      .in("users" / "me")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[User])

  val updateCurrentUser: PublicEndpoint[UserUpdate, ErrorResponse, User, Any] =
    endpoint.patch
      .in("users" / "me")
      .in(jsonBody[UserUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[User])