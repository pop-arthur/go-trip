import domain.location.*
import repository.location.LocationRepository
import service.location.LocationService

@main
def main(): Unit = {
  val locationService = LocationService(LocationRepository.makeInMemory)

  val result = locationService.search(
    LocationSearchParams(
      country = Some("vietnam"),
    )
  )

  result.foreach(println)
}
