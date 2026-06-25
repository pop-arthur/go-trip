package gotrip.http.triplocation

import gotrip.domain.trip.*
import gotrip.http.{EndpointErrors, HttpError}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object TripLocationEndpoints:
  import TripLocationCodecs.given

  type ErrorResponse = HttpError

  val listTripLocations: PublicEndpoint[TripId, ErrorResponse, List[TripLocation], Any] =
    endpoint.get
      .in("trips" / path[TripId]("tripId") / "locations")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[TripLocation]])

  val addTripLocation: PublicEndpoint[(TripId, TripLocationCreate), ErrorResponse, TripLocation, Any] =
    endpoint.post
      .in("trips" / path[TripId]("tripId") / "locations")
      .in(jsonBody[TripLocationCreate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[TripLocation])

  val updateTripLocation
      : PublicEndpoint[(TripId, TripLocationId, TripLocationUpdate), ErrorResponse, TripLocation, Any] =
    endpoint.patch
      .in("trips" / path[TripId]("tripId") / "locations" / path[TripLocationId]("tripLocationId"))
      .in(jsonBody[TripLocationUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[TripLocation])

  val deleteTripLocation: PublicEndpoint[(TripId, TripLocationId), ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("trips" / path[TripId]("tripId") / "locations" / path[TripLocationId]("tripLocationId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
