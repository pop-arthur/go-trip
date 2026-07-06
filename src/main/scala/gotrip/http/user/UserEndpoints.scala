package gotrip.http.user

import gotrip.domain.user.UserId
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.{AuthEndpoints, PublicUser}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import UserCodecs.{UserUpdate, given}

object UserEndpoints:
  import UserCodecs.given

  type ErrorResponse = HttpError

  val getCurrentUser: Endpoint[String, Unit, ErrorResponse, PublicUser, Any] =
    endpoint.get
      .tag(SwaggerTags.Users)
      .securityIn(AuthEndpoints.bearer)
      .in("users" / "me")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[PublicUser])

  val updateCurrentUser: Endpoint[String, UserUpdate, ErrorResponse, PublicUser, Any] =
    endpoint.patch
      .tag(SwaggerTags.Users)
      .securityIn(AuthEndpoints.bearer)
      .in("users" / "me")
      .in(jsonBody[UserUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[PublicUser])
