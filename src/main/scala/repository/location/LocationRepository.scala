package repository.location

import domain.location.*

type Id[A] = A

trait LocationRepository[F[_]]:
  def search(params: LocationSearchParams): F[List[Location]]
  def findAll(): F[List[Location]]
  def findById(id: LocationId): F[Option[Location]]

object LocationRepository:
  
  def makeInMemory: LocationRepository[Id] =
    new LocationRepository[Id]:
      private val locations: List[Location] = List(
        Location(
          id = LocationId(1),
          name = LocationName("Noi Bai International Airport"),
          locationType = LocationType.Airport,
          country = LocationCountry(Some("Vietnam")),
          city = LocationCity(Some("Hanoi")),
          address = LocationAddress(Some("Phu Minh, Soc Son, Hanoi")),
          latitude = LocationLatitude(Some(21.2187)),
          longitude = LocationLongitude(Some(105.8042))
        ),
        Location(
          id = LocationId(2),
          name = LocationName("Hoan Kiem Lake"),
          locationType = LocationType.Attraction,
          country = LocationCountry(Some("Vietnam")),
          city = LocationCity(Some("Hanoi")),
          address = LocationAddress(None),
          latitude = LocationLatitude(None),
          longitude = LocationLongitude(None)
        )
      )

      override def findAll(): List[Location] =
        locations

      override def search(params: LocationSearchParams): List[Location] =
        locations.filter { location =>
          params.locationType.forall(_ == location.locationType) &&
            params.country.forall(country =>
              location.country.value.exists(_.equalsIgnoreCase(country))
            ) &&
            params.city.forall(city =>
              location.city.value.exists(_.equalsIgnoreCase(city))
            ) &&
            params.query.forall(query =>
              location.name.value.toLowerCase.contains(query.toLowerCase)
            )
        }

      override def findById(id: LocationId): Option[Location] =
        locations.find(_.id == id)
