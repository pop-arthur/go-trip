package gotrip.http.triplocation

import gotrip.domain.trip.*
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object TripLocationEndpoints:
  import TripLocationCodecs.given

  type ErrorResponse = HttpError

  val listTripLocations: Endpoint[String, TripId, ErrorResponse, List[TripLocation], Any] =
    endpoint.get
      .tag(SwaggerTags.TripLocations)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "locations")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[TripLocation]])

  val addTripLocation: Endpoint[String, (TripId, TripLocationCreate), ErrorResponse, TripLocation, Any] =
    endpoint.post
      .tag(SwaggerTags.TripLocations)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "locations")
      .in(jsonBody[TripLocationCreate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[TripLocation])

  val updateTripLocation
      : Endpoint[String, (TripId, TripLocationId, TripLocationUpdate), ErrorResponse, TripLocation, Any] =
    endpoint.patch
      .tag(SwaggerTags.TripLocations)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "locations" / path[TripLocationId]("tripLocationId"))
      .in(jsonBody[TripLocationUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[TripLocation])

  val deleteTripLocation: Endpoint[String, (TripId, TripLocationId), ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.TripLocations)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "locations" / path[TripLocationId]("tripLocationId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
