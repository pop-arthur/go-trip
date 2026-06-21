package gotrip.http.location

import gotrip.domain.location.*
import gotrip.http.ApiError
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object LocationEndpoints:
  import LocationCodecs.given

  type ErrorResponse = (StatusCode, ApiError)

  private val errorOut: EndpointOutput[ErrorResponse] =
    statusCode.and(jsonBody[ApiError])

  val listLocations
      : PublicEndpoint[(Option[LocationType], Option[String], Option[String], Option[String]), ErrorResponse, List[Location], Any] =
    endpoint.get
      .in("locations")
      .in(query[Option[LocationType]]("type"))
      .in(query[Option[String]]("country"))
      .in(query[Option[String]]("city"))
      .in(query[Option[String]]("query"))
      .errorOut(errorOut)
      .out(jsonBody[List[Location]])

  val getLocation: PublicEndpoint[LocationId, ErrorResponse, Location, Any] =
    endpoint.get
      .in("locations" / path[LocationId]("locationId"))
      .errorOut(errorOut)
      .out(jsonBody[Location])

  val createLocation: PublicEndpoint[LocationCreate, ErrorResponse, Location, Any] =
    endpoint.post
      .in("locations")
      .in(jsonBody[LocationCreate])
      .errorOut(errorOut)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Location])

  val updateLocation: PublicEndpoint[(LocationId, LocationUpdate), ErrorResponse, Location, Any] =
    endpoint.patch
      .in("locations" / path[LocationId]("locationId"))
      .in(jsonBody[LocationUpdate])
      .errorOut(errorOut)
      .out(jsonBody[Location])

  val deleteLocation: PublicEndpoint[LocationId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("locations" / path[LocationId]("locationId"))
      .errorOut(errorOut)
      .out(statusCode(StatusCode.NoContent))
