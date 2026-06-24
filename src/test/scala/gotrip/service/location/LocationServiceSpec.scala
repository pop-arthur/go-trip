package gotrip.service.location

import cats.Id
import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class LocationServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "LocationService" should {
    "delegate search to repository and return locations" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.search
        .expects(searchParams)
        .returning(List(location))

      service.search(searchParams) shouldBe List(location)
    }

    "delegate findById to repository and return a location when found" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.findById
        .expects(locationId)
        .returning(Some(location))

      service.findById(locationId) shouldBe Some(location)
    }

    "delegate findById to repository and return None when location does not exist" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.findById
        .expects(locationId)
        .returning(None)

      service.findById(locationId) shouldBe None
    }

    "delegate create to repository and return created location" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.create
        .expects(locationCreate)
        .returning(createdLocation)

      service.create(locationCreate) shouldBe createdLocation
    }

    "delegate update to repository and return updated location when found" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.update
        .expects(locationId, locationUpdate)
        .returning(Some(updatedLocation))

      service.update(locationId, locationUpdate) shouldBe Some(updatedLocation)
    }

    "delegate update to repository and return None when location does not exist" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.update
        .expects(locationId, locationUpdate)
        .returning(None)

      service.update(locationId, locationUpdate) shouldBe None
    }

    "delegate delete to repository and return true when deleted" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.delete
        .expects(locationId)
        .returning(true)

      service.delete(locationId) shouldBe true
    }

    "delegate delete to repository and return false when location does not exist" in {
      val repository = mock[LocationRepository[Id]]
      val service = LocationService[Id](repository)

      repository.delete
        .expects(locationId)
        .returning(false)

      service.delete(locationId) shouldBe false
    }
  }

  private val locationId: LocationId = LocationId(1L)

  private val location: Location = Location(
    id = locationId,
    name = LocationName("Noi Bai International Airport"),
    `type` = LocationType.Airport,
    country = LocationCountry(Some("Vietnam")),
    city = LocationCity(Some("Hanoi")),
    address = LocationAddress(Some("Phu Minh, Soc Son, Hanoi")),
    latitude = LocationLatitude(Some(21.2187)),
    longitude = LocationLongitude(Some(105.8042))
  )

  private val createdLocation: Location = location.copy(id = LocationId(2L))

  private val updatedLocation: Location = location.copy(
    name = LocationName("Hoan Kiem Lake"),
    `type` = LocationType.Attraction,
    address = LocationAddress(None),
    latitude = LocationLatitude(None),
    longitude = LocationLongitude(None)
  )

  private val locationCreate: LocationCreate = LocationCreate(
    name = LocationName("Hoan Kiem Lake"),
    `type` = LocationType.Attraction,
    country = LocationCountry(Some("Vietnam")),
    city = LocationCity(Some("Hanoi")),
    address = LocationAddress(None),
    latitude = LocationLatitude(None),
    longitude = LocationLongitude(None)
  )

  private val locationUpdate: LocationUpdate = LocationUpdate(
    name = Some(LocationName("Hoan Kiem Lake")),
    `type` = Some(LocationType.Attraction),
    address = Some(LocationAddress(None)),
    latitude = Some(LocationLatitude(None)),
    longitude = Some(LocationLongitude(None))
  )

  private val searchParams: LocationSearchParams = LocationSearchParams(
    `type` = Some(LocationType.Airport),
    country = Some("Vietnam"),
    city = Some("Hanoi"),
    query = Some("noi bai")
  )
