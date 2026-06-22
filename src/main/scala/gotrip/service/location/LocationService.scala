package gotrip.service.location

import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository

final class LocationService[F[_]](repository: LocationRepository[F]):

  def search(params: LocationSearchParams): F[List[Location]] =
    repository.search(params)

  def findById(id: LocationId): F[Option[Location]] =
    repository.findById(id)

  def create(location: LocationCreate): F[Location] =
    repository.create(location)

  def update(id: LocationId, location: LocationUpdate): F[Option[Location]] =
    repository.update(id, location)

  def delete(id: LocationId): F[Boolean] =
    repository.delete(id)
