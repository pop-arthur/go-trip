package gotrip.repository.additionalservice

import cats.effect.{Concurrent, Resource}
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import skunk.Session

trait AdditionalServiceRepository[F[_]]:
  def search(params: AdditionalServiceSearchParams): F[List[AdditionalService]]
  def findById(id: ServiceId): F[Option[AdditionalService]]
  def create(service: AdditionalService): F[AdditionalService]
  def update(id: ServiceId, service: AdditionalServiceUpdate): F[Option[AdditionalService]]
  def delete(id: ServiceId): F[Boolean]
  def providerExists(id: ProviderId): F[Boolean]
  def locationExists(id: LocationId): F[Boolean]

object AdditionalServiceRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): AdditionalServiceRepository[F] =
    PostgresAdditionalServiceRepository.make(sessionPool)
