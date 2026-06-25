package gotrip.http.auth

import gotrip.http.{EndpointErrors, HttpError}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

object AuthEndpoints:
  import AuthCodecs.given
  import HttpError.given

  type ErrorResponse = HttpError

  val bearer =
    auth.bearer[String]()

  val register: PublicEndpoint[RegisterRequest, ErrorResponse, AuthResponse, Any] =
    endpoint.post
      .in("auth" / "register")
      .in(jsonBody[RegisterRequest])
      .errorOut(EndpointErrors.validationOrConflict)
      .out(statusCode(StatusCode.Created).and(jsonBody[AuthResponse]))

  val login: PublicEndpoint[LoginRequest, ErrorResponse, AuthResponse, Any] =
    endpoint.post
      .in("auth" / "login")
      .in(jsonBody[LoginRequest])
      .errorOut(EndpointErrors.validation)
      .out(jsonBody[AuthResponse])

  val refresh: PublicEndpoint[RefreshRequest, ErrorResponse, AuthResponse, Any] =
    endpoint.post
      .in("auth" / "refresh")
      .in(jsonBody[RefreshRequest])
      .errorOut(EndpointErrors.validation)
      .out(jsonBody[AuthResponse])

  val logout: Endpoint[String, Unit, ErrorResponse, Unit, Any] =
    endpoint.post
      .securityIn(bearer)
      .in("auth" / "logout")
      .errorOut(EndpointErrors.internalOnly)
      .out(statusCode(StatusCode.NoContent))
