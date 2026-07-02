package gotrip.repository.location

import cats.effect.{Concurrent, Resource}
import gotrip.domain.location.*
import skunk.Session


trait LocationRepository[F[_]]:
  def search(params: LocationSearchParams): F[List[Location]]
  def findAll(): F[List[Location]]
  def findById(id: LocationId): F[Option[Location]]
  def create(location: Location): F[Location]
  def update(id: LocationId, location: LocationUpdate): F[Option[Location]]
  def delete(id: LocationId): F[Boolean]

object LocationRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): LocationRepository[F] =
    PostgresLocationRepository.make(sessionPool)
