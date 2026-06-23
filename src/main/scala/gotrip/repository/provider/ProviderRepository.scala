package gotrip.repository.provider

import cats.effect.{Concurrent, Resource}
import gotrip.domain.provider.*
import skunk.Session

trait ProviderRepository[F[_]]:
  def search(params: ProviderSearchParams): F[List[Provider]]
  def findById(id: ProviderId): F[Option[Provider]]
  def create(provider: ProviderCreate): F[Provider]
  def update(id: ProviderId, provider: ProviderUpdate): F[Option[Provider]]
  def delete(id: ProviderId): F[Boolean]
  def nameExists(name: ProviderName, excludeProviderId: Option[ProviderId] = None): F[Boolean]
  def hasAdditionalServices(id: ProviderId): F[Boolean]

object ProviderRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): ProviderRepository[F] =
    PostgresProviderRepository.make(sessionPool)
