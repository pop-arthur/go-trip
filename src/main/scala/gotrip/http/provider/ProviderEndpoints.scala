package gotrip.http.provider

import gotrip.domain.provider.*
import gotrip.http.ApiError
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object ProviderEndpoints:
  import ProviderCodecs.given

  type ErrorResponse = (StatusCode, ApiError)

  private val errorOut: EndpointOutput[ErrorResponse] =
    statusCode.and(jsonBody[ApiError])

  val listProviders: PublicEndpoint[(Option[ProviderType], Option[String]), ErrorResponse, List[Provider], Any] =
    endpoint.get
      .in("providers")
      .in(query[Option[ProviderType]]("type"))
      .in(query[Option[String]]("query"))
      .errorOut(errorOut)
      .out(jsonBody[List[Provider]])

  val getProvider: PublicEndpoint[ProviderId, ErrorResponse, Provider, Any] =
    endpoint.get
      .in("providers" / path[ProviderId]("providerId"))
      .errorOut(errorOut)
      .out(jsonBody[Provider])

  val createProvider: PublicEndpoint[ProviderCreate, ErrorResponse, Provider, Any] =
    endpoint.post
      .in("providers")
      .in(jsonBody[ProviderCreate])
      .errorOut(errorOut)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Provider])

  val deleteProvider: PublicEndpoint[ProviderId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("providers" / path[ProviderId]("providerId"))
      .errorOut(errorOut)
      .out(statusCode(StatusCode.NoContent))

  val adminCreateProvider: PublicEndpoint[ProviderCreate, ErrorResponse, Provider, Any] =
    endpoint.post
      .in("admin" / "providers")
      .in(jsonBody[ProviderCreate])
      .errorOut(errorOut)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Provider])

  val adminUpdateProvider: PublicEndpoint[(ProviderId, ProviderUpdate), ErrorResponse, Provider, Any] =
    endpoint.patch
      .in("admin" / "providers" / path[ProviderId]("providerId"))
      .in(jsonBody[ProviderUpdate])
      .errorOut(errorOut)
      .out(jsonBody[Provider])

  val adminDeleteProvider: PublicEndpoint[ProviderId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("admin" / "providers" / path[ProviderId]("providerId"))
      .errorOut(errorOut)
      .out(statusCode(StatusCode.NoContent))
