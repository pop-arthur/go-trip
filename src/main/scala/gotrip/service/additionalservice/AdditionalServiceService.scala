package gotrip.service.additionalservice

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.repository.additionalservice.AdditionalServiceRepository

final class AdditionalServiceService[F[_]: Monad](repository: AdditionalServiceRepository[F]):

  import AdditionalServiceServiceError.*

  def search(params: AdditionalServiceSearchParams): F[List[AdditionalService]] =
    repository.search(params)

  def findById(id: ServiceId): F[Option[AdditionalService]] =
    repository.findById(id)

  def create(service: AdditionalServiceCreate): F[Either[AdditionalServiceServiceError, AdditionalService]] =
    (for {
      _ <- ensureProviderExists(service.provider_id)
      _ <- ensureLocationExists(service.location_id)
      created <- EitherT.liftF(repository.create(service))
    } yield created).value

  def update(
    id: ServiceId,
    service: AdditionalServiceUpdate
  ): F[Either[AdditionalServiceServiceError, AdditionalService]] =
    (for {
      _ <- EitherT.fromOptionF(repository.findById(id), AdditionalServiceNotFound(id))
      _ <- ensureProviderExists(service.provider_id)
      _ <- ensureLocationExists(service.location_id)
      updated <- EitherT.fromOptionF(repository.update(id, service), AdditionalServiceNotFound(id))
    } yield updated).value

  def delete(id: ServiceId): F[Either[AdditionalServiceServiceError, Unit]] =
    (for {
      deleted <- EitherT.liftF(repository.delete(id))
      _ <- if deleted then EitherT.rightT[F, AdditionalServiceServiceError](())
           else EitherT.leftT[F, Unit](AdditionalServiceNotFound(id))
    } yield ()).value

  private def ensureProviderExists(
    providerId: Option[ProviderId]
  ): EitherT[F, AdditionalServiceServiceError, Unit] =
    providerId.fold(EitherT.rightT[F, AdditionalServiceServiceError](())) { id =>
      EitherT {
        repository.providerExists(id).map { exists =>
          Either.cond(exists, (), ProviderNotFound(id))
        }
      }
    }

  private def ensureLocationExists(
    locationId: Option[LocationId]
  ): EitherT[F, AdditionalServiceServiceError, Unit] =
    locationId.fold(EitherT.rightT[F, AdditionalServiceServiceError](())) { id =>
      EitherT {
        repository.locationExists(id).map { exists =>
          Either.cond(exists, (), LocationNotFound(id))
        }
      }
    }

enum AdditionalServiceServiceError:
  case AdditionalServiceNotFound(id: ServiceId)
  case ProviderNotFound(id: ProviderId)
  case LocationNotFound(id: LocationId)
