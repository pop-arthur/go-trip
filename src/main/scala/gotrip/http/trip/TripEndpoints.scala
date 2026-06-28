package gotrip.http.trip

import gotrip.domain.trip.*
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

import java.time.LocalDate

object TripEndpoints:
  import TripCodecs.given

  type ErrorResponse = HttpError

  val listTrips: Endpoint[String, (Option[TripStatus], Option[LocalDate], Option[LocalDate]), ErrorResponse, List[Trip], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("trips")
      .in(query[Option[TripStatus]]("status"))
      .in(query[Option[LocalDate]]("fromDate"))
      .in(query[Option[LocalDate]]("toDate"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Trip]])

  val createTrip: Endpoint[String, TripCreate, ErrorResponse, Trip, Any] =
    endpoint.post
      .securityIn(AuthEndpoints.bearer)
      .in("trips")
      .in(jsonBody[TripCreate])
      .errorOut(EndpointErrors.validation)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Trip])

  val getTrip: Endpoint[String, TripId, ErrorResponse, Trip, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Trip])

  val updateTrip: Endpoint[String, (TripId, TripUpdate), ErrorResponse, Trip, Any] =
    endpoint.patch
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId"))
      .in(jsonBody[TripUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Trip])

  val deleteTrip: Endpoint[String, TripId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId"))
      .errorOut(EndpointErrors.notFoundOrConflict)
      .out(statusCode(StatusCode.NoContent))
