package gotrip.service.provider

import cats.Monad
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.provider.*
import gotrip.repository.provider.ProviderRepository
import gotrip.service.GeneratedData

trait ProviderService[F[_]] {
  def search(params: ProviderSearchParams): F[List[Provider]]
  def findById(id: ProviderId): F[Option[Provider]]
  def create(provider: ProviderCreate): F[Either[ProviderServiceError, Provider]]
  def update(id: ProviderId, provider: ProviderUpdate): F[Either[ProviderServiceError, Provider]]
  def delete(id: ProviderId): F[Either[ProviderServiceError, Unit]]
}

object ProviderService {
  def make[F[_]: Sync: GeneratedData](repo: ProviderRepository[F]): ProviderService[F] =
    new ProviderService[F] {
      import ProviderServiceError.*

      override def search(params: ProviderSearchParams): F[List[Provider]] =
        repo.search(params)

      override def findById(id: ProviderId): F[Option[Provider]] =
        repo.findById(id)

      override def create(provider: ProviderCreate): F[Either[ProviderServiceError, Provider]] =
        (for {
          _ <- ensureNameAvailable(provider.name, None)
          materialized <- EitherT.liftF(materializeProvider(provider))
          created <- EitherT.liftF(repo.create(materialized))
        } yield created).value

      override def update(id: ProviderId, provider: ProviderUpdate): F[Either[ProviderServiceError, Provider]] =
        (for {
          _ <- EitherT.fromOptionF(repo.findById(id), ProviderNotFound(id))
          _ <- provider.name.fold(EitherT.rightT[F, ProviderServiceError](())) { name =>
            ensureNameAvailable(name, Some(id))
          }
          updated <- EitherT.fromOptionF(repo.update(id, provider), ProviderNotFound(id))
        } yield updated).value

      override def delete(id: ProviderId): F[Either[ProviderServiceError, Unit]] =
        (for {
          _ <- EitherT.fromOptionF(repo.findById(id), ProviderNotFound(id))
          _ <- ensureNotInUse(id)
          deleted <- EitherT.liftF(repo.delete(id))
          _ <- if deleted then EitherT.rightT[F, ProviderServiceError](())
               else EitherT.leftT[F, Unit](ProviderNotFound(id))
        } yield ()).value

      private def ensureNameAvailable(
        name: ProviderName,
        excludeProviderId: Option[ProviderId]
      ): EitherT[F, ProviderServiceError, Unit] =
        EitherT {
          repo.nameExists(name, excludeProviderId).map { exists =>
            Either.cond(!exists, (), DuplicateProviderName(name))
          }
        }

      private def ensureNotInUse(id: ProviderId): EitherT[F, ProviderServiceError, Unit] =
        EitherT {
          repo.hasAdditionalServices(id).map { inUse =>
            Either.cond(!inUse, (), ProviderInUse(id))
          }
        }

      private def materializeProvider(create: ProviderCreate): F[Provider] =
        GeneratedData[F].newId().map { id =>
          Provider(
            id = ProviderId(id),
            name = create.name,
            `type` = create.`type`,
            website = create.website,
            support_contact = create.support_contact
          )
        }
    }
}

enum ProviderServiceError {
  case ProviderNotFound(id: ProviderId)
  case DuplicateProviderName(name: ProviderName)
  case ProviderInUse(id: ProviderId)
}