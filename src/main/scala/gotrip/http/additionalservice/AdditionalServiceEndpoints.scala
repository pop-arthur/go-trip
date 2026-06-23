package gotrip.http.additionalservice

import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.http.ApiError
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object AdditionalServiceEndpoints:
  import AdditionalServiceCodecs.given

  type ErrorResponse = (StatusCode, ApiError)

  private val errorOut: EndpointOutput[ErrorResponse] =
    statusCode.and(jsonBody[ApiError])

  val listAdditionalServices
      : PublicEndpoint[(Option[ServiceType], Option[LocationId], Option[ProviderId]), ErrorResponse, List[AdditionalService], Any] =
    endpoint.get
      .in("additional-services")
      .in(query[Option[ServiceType]]("serviceType"))
      .in(query[Option[LocationId]]("locationId"))
      .in(query[Option[ProviderId]]("providerId"))
      .errorOut(errorOut)
      .out(jsonBody[List[AdditionalService]])

  val getAdditionalService: PublicEndpoint[ServiceId, ErrorResponse, AdditionalService, Any] =
    endpoint.get
      .in("additional-services" / path[ServiceId]("serviceId"))
      .errorOut(errorOut)
      .out(jsonBody[AdditionalService])

  val adminCreateAdditionalService: PublicEndpoint[AdditionalServiceCreate, ErrorResponse, AdditionalService, Any] =
    endpoint.post
      .in("admin" / "additional-services")
      .in(jsonBody[AdditionalServiceCreate])
      .errorOut(errorOut)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[AdditionalService])

  val adminUpdateAdditionalService
      : PublicEndpoint[(ServiceId, AdditionalServiceUpdate), ErrorResponse, AdditionalService, Any] =
    endpoint.patch
      .in("admin" / "additional-services" / path[ServiceId]("serviceId"))
      .in(jsonBody[AdditionalServiceUpdate])
      .errorOut(errorOut)
      .out(jsonBody[AdditionalService])

  val adminDeleteAdditionalService: PublicEndpoint[ServiceId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("admin" / "additional-services" / path[ServiceId]("serviceId"))
      .errorOut(errorOut)
      .out(statusCode(StatusCode.NoContent))
