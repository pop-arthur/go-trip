package repository.location

import domain.location.*

trait LocationRepository:
  def findAll(): List[Location]
  def findById(id: LocationId): Option[Location]

object InMemoryLocationRepository extends LocationRepository:
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

  override def findById(id: LocationId): Option[Location] =
    locations.find(_.id == id)
