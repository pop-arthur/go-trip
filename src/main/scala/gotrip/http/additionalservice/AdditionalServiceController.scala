package gotrip.http.additionalservice

import cats.effect.IO
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.service.additionalservice.{AdditionalServiceService, AdditionalServiceServiceError}
import sttp.tapir.server.ServerEndpoint

final class AdditionalServiceController(service: AdditionalServiceService[IO]):

  val listAdditionalServices: ServerEndpoint[Any, IO] =
    AdditionalServiceEndpoints.listAdditionalServices.serverLogic { case (serviceType, locationId, providerId) =>
      val params = AdditionalServiceSearchParams(
        serviceType = serviceType,
        locationId = locationId,
        providerId = providerId
      )

      service.search(params).attempt.map {
        case Right(services) =>
          Right(services)

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val getAdditionalService: ServerEndpoint[Any, IO] =
    AdditionalServiceEndpoints.getAdditionalService.serverLogic { id =>
      service.findById(id).attempt.map {
        case Right(Some(additionalService)) =>
          Right(additionalService)

        case Right(None) =>
          Left(notFound(id))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val adminCreateAdditionalService: ServerEndpoint[Any, IO] =
    AdditionalServiceEndpoints.adminCreateAdditionalService.serverLogic { additionalService =>
      AdditionalServiceValidator.validate(additionalService).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validService) =>
          service.create(validService).attempt.map {
            case Right(Right(created)) =>
              Right(created)

            case Right(Left(error)) =>
              Left(serviceError(error))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val adminUpdateAdditionalService: ServerEndpoint[Any, IO] =
    AdditionalServiceEndpoints.adminUpdateAdditionalService.serverLogic { case (id, additionalService) =>
      AdditionalServiceValidator.validate(additionalService).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validService) =>
          service.update(id, validService).attempt.map {
            case Right(Right(updated)) =>
              Right(updated)

            case Right(Left(error)) =>
              Left(serviceError(error))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val adminDeleteAdditionalService: ServerEndpoint[Any, IO] =
    AdditionalServiceEndpoints.adminDeleteAdditionalService.serverLogic { id =>
      service.delete(id).attempt.map {
        case Right(Right(_)) =>
          Right(())

        case Right(Left(error)) =>
          Left(serviceError(error))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(
      listAdditionalServices,
      getAdditionalService,
      adminCreateAdditionalService,
      adminUpdateAdditionalService,
      adminDeleteAdditionalService
    )

  private def serviceError(error: AdditionalServiceServiceError): AdditionalServiceEndpoints.ErrorResponse =
    error match
      case AdditionalServiceServiceError.AdditionalServiceNotFound(id) =>
        notFound(id)

      case AdditionalServiceServiceError.ProviderNotFound(id) =>
        notFound(providerNotFoundMessage(id))

      case AdditionalServiceServiceError.LocationNotFound(id) =>
        notFound(locationNotFoundMessage(id))

  private def notFound(id: ServiceId): AdditionalServiceEndpoints.ErrorResponse =
    notFound(s"Additional service with id ${id.value} was not found")

  private def providerNotFoundMessage(id: ProviderId): String =
    s"Provider with id ${id.value} was not found"

  private def locationNotFoundMessage(id: LocationId): String =
    s"Location with id ${id.value} was not found"

  private def notFound(message: String): AdditionalServiceEndpoints.ErrorResponse =
    HttpError.NotFound(message)

  private def internalError(error: Throwable): AdditionalServiceEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
