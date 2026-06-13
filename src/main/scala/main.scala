import domain.location.*
import repository.location.InMemoryLocationRepository
import service.location.LocationService

@main
def main(): Unit = {
  val locationService = LocationService(InMemoryLocationRepository)

  val result = locationService.search(
    LocationSearchParams(
      country = Some("vietnam"),
    )
  )

  result.foreach(println)
}
