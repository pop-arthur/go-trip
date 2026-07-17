package gotrip.service.location

import cats.effect.Sync
import cats.syntax.flatMap.*
import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository
import gotrip.service.GeneratedData

trait LocationService[F[_]] {
  def search(params: LocationSearchParams): F[List[Location]]
  def findById(id: LocationId): F[Option[Location]]
  def create(location: LocationCreate): F[Location]
  def update(id: LocationId, location: LocationUpdate): F[Option[Location]]
  def delete(id: LocationId): F[Boolean]
}

object LocationService {
  def make[F[_]: Sync: GeneratedData](repo: LocationRepository[F]): LocationService[F] =
    new LocationService[F] {
      override def search(params: LocationSearchParams): F[List[Location]] =
        repo.search(params)

      override def findById(id: LocationId): F[Option[Location]] =
        repo.findById(id)

      override def create(location: LocationCreate): F[Location] =
        GeneratedData[F].newId().flatMap { id =>
          repo.create(
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

      override def update(id: LocationId, location: LocationUpdate): F[Option[Location]] =
        repo.update(id, location)

      override def delete(id: LocationId): F[Boolean] =
        repo.delete(id)
    }
}