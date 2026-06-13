package service.location

import domain.location.*
import repository.location.LocationRepository

final class LocationService(repository: LocationRepository):

  def search(params: LocationSearchParams): List[Location] =
    val locations = repository.findAll()

    locations.filter { location =>
      params.locationType.forall(_ == location.locationType) &&
      params.country.forall(c =>
        location.country.value.exists(_.equalsIgnoreCase(c))
      ) &&
      params.city.forall(c =>
        location.city.value.exists(_.equalsIgnoreCase(c))
      ) &&
      params.query.forall(q => location.name.value.toLowerCase.contains(q.toLowerCase))
    }

  def findById(id: LocationId): Option[Location] =
    repository.findById(id)
