package gotrip.repository.location

import cats.effect.{Concurrent, Resource}
import gotrip.domain.location.*
import skunk.Session
import cats.Id


trait LocationRepository[F[_]]:
  def search(params: LocationSearchParams): F[List[Location]]
  def findAll(): F[List[Location]]
  def findById(id: LocationId): F[Option[Location]]

object LocationRepository:

  def makeInMemory: LocationRepository[Id] =
    InMemoryLocationRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): LocationRepository[F] =
    PostgresLocationRepository.make(sessionPool)
