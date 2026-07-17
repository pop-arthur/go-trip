package gotrip.service.additionalservice

import cats.Monad
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.functor.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.service.GeneratedData

trait AdditionalServiceService[F[_]] {
  def search(params: AdditionalServiceSearchParams): F[List[AdditionalService]]
  def findById(id: ServiceId): F[Option[AdditionalService]]
  def create(service: AdditionalServiceCreate): F[Either[AdditionalServiceServiceError, AdditionalService]]
  def update(id: ServiceId, service: AdditionalServiceUpdate): F[Either[AdditionalServiceServiceError, AdditionalService]]
  def delete(id: ServiceId): F[Either[AdditionalServiceServiceError, Unit]]
}

object AdditionalServiceService {
  def make[F[_]: Sync: GeneratedData](repo: AdditionalServiceRepository[F]): AdditionalServiceService[F] =
    new AdditionalServiceService[F] {
      import AdditionalServiceServiceError.*

      override def search(params: AdditionalServiceSearchParams): F[List[AdditionalService]] =
        repo.search(params)

      override def findById(id: ServiceId): F[Option[AdditionalService]] =
        repo.findById(id)

      override def create(service: AdditionalServiceCreate): F[Either[AdditionalServiceServiceError, AdditionalService]] =
        (for {
          _ <- ensureProviderExists(service.provider_id)
          _ <- ensureLocationExists(service.location_id)
          materialized <- EitherT.liftF(materializeService(service))
          created <- EitherT.liftF(repo.create(materialized))
        } yield created).value

      override def update(id: ServiceId, service: AdditionalServiceUpdate): F[Either[AdditionalServiceServiceError, AdditionalService]] =
        (for {
          _ <- EitherT.fromOptionF(repo.findById(id), AdditionalServiceNotFound(id))
          _ <- ensureProviderExists(service.provider_id)
          _ <- ensureLocationExists(service.location_id)
          updated <- EitherT.fromOptionF(repo.update(id, service), AdditionalServiceNotFound(id))
        } yield updated).value

      override def delete(id: ServiceId): F[Either[AdditionalServiceServiceError, Unit]] =
        (for {
          deleted <- EitherT.liftF(repo.delete(id))
          _ <- if deleted then EitherT.rightT[F, AdditionalServiceServiceError](())
               else EitherT.leftT[F, Unit](AdditionalServiceNotFound(id))
        } yield ()).value

      private def ensureProviderExists(
        providerId: Option[ProviderId]
      ): EitherT[F, AdditionalServiceServiceError, Unit] =
        providerId.fold(EitherT.rightT[F, AdditionalServiceServiceError](())) { id =>
          EitherT {
            repo.providerExists(id).map { exists =>
              Either.cond(exists, (), ProviderNotFound(id))
            }
          }
        }

      private def ensureLocationExists(
        locationId: Option[LocationId]
      ): EitherT[F, AdditionalServiceServiceError, Unit] =
        locationId.fold(EitherT.rightT[F, AdditionalServiceServiceError](())) { id =>
          EitherT {
            repo.locationExists(id).map { exists =>
              Either.cond(exists, (), LocationNotFound(id))
            }
          }
        }

      private def materializeService(create: AdditionalServiceCreate): F[AdditionalService] =
        GeneratedData[F].newId().map { id =>
          AdditionalService(
            id = ServiceId(id),
            title = create.title,
            description = create.description,
            service_type = create.service_type,
            provider_id = create.provider_id,
            location_id = create.location_id,
            price_amount = create.price_amount,
            price_currency = create.price_currency,
            is_active = create.is_active.getOrElse(true)
          )
        }
    }
}

enum AdditionalServiceServiceError:
  case AdditionalServiceNotFound(id: ServiceId)
  case ProviderNotFound(id: ProviderId)
  case LocationNotFound(id: LocationId)