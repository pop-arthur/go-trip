package gotrip.http.provider

import cats.effect.IO
import gotrip.domain.provider.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.service.provider.{ProviderService, ProviderServiceError}
import sttp.tapir.server.ServerEndpoint

final class ProviderController(service: ProviderService[IO]):

  val listProviders: ServerEndpoint[Any, IO] =
    ProviderEndpoints.listProviders.serverLogic { case (providerType, query) =>
      service.search(ProviderSearchParams(providerType, query)).attempt.map {
        case Right(providers) =>
          Right(providers)

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val getProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.getProvider.serverLogic { id =>
      service.findById(id).attempt.map {
        case Right(Some(provider)) =>
          Right(provider)

        case Right(None) =>
          Left(notFound(id))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val createProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.createProvider.serverLogic { provider =>
      create(provider)
    }

  val deleteProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.deleteProvider.serverLogic { id =>
      delete(id)
    }

  val adminCreateProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.adminCreateProvider.serverLogic { provider =>
      create(provider)
    }

  val adminUpdateProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.adminUpdateProvider.serverLogic { case (id, provider) =>
      ProviderValidator.validate(provider).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validProvider) =>
          service.update(id, validProvider).attempt.map {
            case Right(Right(updated)) =>
              Right(updated)

            case Right(Left(error)) =>
              Left(serviceError(error))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val adminDeleteProvider: ServerEndpoint[Any, IO] =
    ProviderEndpoints.adminDeleteProvider.serverLogic { id =>
      delete(id)
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(
      listProviders,
      getProvider,
      createProvider,
      deleteProvider,
      adminCreateProvider,
      adminUpdateProvider,
      adminDeleteProvider
    )

  private def create(provider: ProviderCreate): IO[Either[ProviderEndpoints.ErrorResponse, Provider]] =
    ProviderValidator.validate(provider).toEither match
      case Left(errors) =>
        IO.pure(Left(ValidationError.toHttpError(errors)))
      case Right(validProvider) =>
        service.create(validProvider).attempt.map {
          case Right(Right(created)) =>
            Right(created)

          case Right(Left(error)) =>
            Left(serviceError(error))

          case Left(error) =>
            Left(internalError(error))
        }

  private def delete(id: ProviderId): IO[Either[ProviderEndpoints.ErrorResponse, Unit]] =
    service.delete(id).attempt.map {
      case Right(Right(_)) =>
        Right(())

      case Right(Left(error)) =>
        Left(serviceError(error))

      case Left(error) =>
        Left(internalError(error))
    }

  private def serviceError(error: ProviderServiceError): ProviderEndpoints.ErrorResponse =
    error match
      case ProviderServiceError.ProviderNotFound(id) =>
        notFound(id)

      case ProviderServiceError.DuplicateProviderName(name) =>
        HttpError.Conflict(s"Provider with name '${name.value}' already exists")

      case ProviderServiceError.ProviderInUse(id) =>
        HttpError.Conflict(s"Provider with id ${id.value} is used by additional services")

  private def notFound(id: ProviderId): ProviderEndpoints.ErrorResponse =
    HttpError.NotFound(s"Provider with id ${id.value} was not found")

  private def internalError(error: Throwable): ProviderEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
