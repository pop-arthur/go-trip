package gotrip.service.location

import cats.effect.Sync
import cats.syntax.flatMap.*
import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository
import gotrip.service.GeneratedData

final class LocationService[F[_]: Sync: GeneratedData](repository: LocationRepository[F]):

  def search(params: LocationSearchParams): F[List[Location]] =
    repository.search(params)

  def findById(id: LocationId): F[Option[Location]] =
    repository.findById(id)

  def create(location: LocationCreate): F[Location] =
    GeneratedData[F].newId().flatMap { id =>
      repository.create(
        Location(
          id = LocationId(id),
          name = location.name,
          `type` = location.`type`,
          country = location.country,
          city = location.city,
          address = location.address,
          latitude = location.latitude,
          longitude = location.longitude
        )
      )
    }

  def update(id: LocationId, location: LocationUpdate): F[Option[Location]] =
    repository.update(id, location)

  def delete(id: LocationId): F[Boolean] =
    repository.delete(id)
