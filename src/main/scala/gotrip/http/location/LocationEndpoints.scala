package gotrip.http.location

import gotrip.domain.location.*
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object LocationEndpoints:
  import LocationCodecs.given

  type ErrorResponse = HttpError

  val listLocations
      : Endpoint[String, (Option[LocationType], Option[String], Option[String], Option[String]), ErrorResponse, List[Location], Any] =
    endpoint.get
      .tag(SwaggerTags.Locations)
      .securityIn(AuthEndpoints.bearer)
      .in("locations")
      .in(query[Option[LocationType]]("type"))
      .in(query[Option[String]]("country"))
      .in(query[Option[String]]("city"))
      .in(query[Option[String]]("query"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Location]])

  val getLocation: Endpoint[String, LocationId, ErrorResponse, Location, Any] =
    endpoint.get
      .tag(SwaggerTags.Locations)
      .securityIn(AuthEndpoints.bearer)
      .in("locations" / path[LocationId]("locationId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Location])

  val createLocation: Endpoint[String, LocationCreate, ErrorResponse, Location, Any] =
    endpoint.post
      .tag(SwaggerTags.Locations)
      .securityIn(AuthEndpoints.bearer)
      .in("locations")
      .in(jsonBody[LocationCreate])
      .errorOut(EndpointErrors.validation)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Location])

  val updateLocation: Endpoint[String, (LocationId, LocationUpdate), ErrorResponse, Location, Any] =
    endpoint.patch
      .tag(SwaggerTags.Locations)
      .securityIn(AuthEndpoints.bearer)
      .in("locations" / path[LocationId]("locationId"))
      .in(jsonBody[LocationUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Location])

  val deleteLocation: Endpoint[String, LocationId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.Locations)
      .securityIn(AuthEndpoints.bearer)
      .in("locations" / path[LocationId]("locationId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
