package gotrip.http.provider

import gotrip.domain.provider.*
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object ProviderEndpoints:
  import ProviderCodecs.given

  type ErrorResponse = HttpError

  val listProviders: Endpoint[String, (Option[ProviderType], Option[String]), ErrorResponse, List[Provider], Any] =
    endpoint.get
      .tag(SwaggerTags.Providers)
      .securityIn(AuthEndpoints.bearer)
      .in("providers")
      .in(query[Option[ProviderType]]("type"))
      .in(query[Option[String]]("query"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Provider]])

  val getProvider: Endpoint[String, ProviderId, ErrorResponse, Provider, Any] =
    endpoint.get
      .tag(SwaggerTags.Providers)
      .securityIn(AuthEndpoints.bearer)
      .in("providers" / path[ProviderId]("providerId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Provider])

  val createProvider: Endpoint[String, ProviderCreate, ErrorResponse, Provider, Any] =
    endpoint.post
      .tag(SwaggerTags.Providers)
      .securityIn(AuthEndpoints.bearer)
      .in("providers")
      .in(jsonBody[ProviderCreate])
      .errorOut(EndpointErrors.validationOrConflict)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Provider])

  val deleteProvider: Endpoint[String, ProviderId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.Providers)
      .securityIn(AuthEndpoints.bearer)
      .in("providers" / path[ProviderId]("providerId"))
      .errorOut(EndpointErrors.notFoundOrConflict)
      .out(statusCode(StatusCode.NoContent))

  val adminCreateProvider: Endpoint[String, ProviderCreate, ErrorResponse, Provider, Any] =
    endpoint.post
      .tag(SwaggerTags.AdminProviders)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "providers")
      .in(jsonBody[ProviderCreate])
      .errorOut(EndpointErrors.validationOrConflict)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Provider])

  val adminUpdateProvider: Endpoint[String, (ProviderId, ProviderUpdate), ErrorResponse, Provider, Any] =
    endpoint.patch
      .tag(SwaggerTags.AdminProviders)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "providers" / path[ProviderId]("providerId"))
      .in(jsonBody[ProviderUpdate])
      .errorOut(EndpointErrors.validationOrNotFoundOrConflict)
      .out(jsonBody[Provider])

  val adminDeleteProvider: Endpoint[String, ProviderId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.AdminProviders)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "providers" / path[ProviderId]("providerId"))
      .errorOut(EndpointErrors.notFoundOrConflict)
      .out(statusCode(StatusCode.NoContent))
