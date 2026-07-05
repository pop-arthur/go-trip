package gotrip.repository

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository

final class LocationRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("LocationRepository creates, finds, and lists locations") {
    val locations = LocationRepository.makePostgres[IO](sessionPool)

    for
      hanoi <- locations.create(sampleLocation(21, "Noi Bai International Airport", LocationType.Airport))
      paris <- locations.create(sampleLocation(22, "Paris", LocationType.City, country = Some("France"), city = Some("Paris")))
      byId <- locations.findById(hanoi.id)
      all <- locations.findAll()
    yield
      assertEquals(byId, Some(hanoi))
      assertEquals(all.map(_.id), List(hanoi.id, paris.id))
  }

  repositoryTest("LocationRepository searches by type, country, city, and query") {
    val locations = LocationRepository.makePostgres[IO](sessionPool)

    for
      hanoi <- locations.create(sampleLocation(21, "Noi Bai International Airport", LocationType.Airport))
      _ <- locations.create(sampleLocation(22, "Paris", LocationType.City, country = Some("France"), city = Some("Paris")))
      search <- locations.search(LocationSearchParams(`type` = Some(LocationType.Airport), country = Some("Vietnam"), city = Some("Hanoi"), query = Some("noi")))
    yield assertEquals(search, List(hanoi))
  }

  repositoryTest("LocationRepository partially updates locations") {
    val locations = LocationRepository.makePostgres[IO](sessionPool)

    for
      hanoi <- locations.create(sampleLocation(21, "Noi Bai International Airport", LocationType.Airport))
      updated <- locations.update(hanoi.id, LocationUpdate(name = Some(LocationName("Noi Bai Airport")), address = Some(LocationAddress(None))))
    yield
      assertEquals(updated.map(_.name), Some(LocationName("Noi Bai Airport")))
      assertEquals(updated.flatMap(_.address.value), None)
  }

  repositoryTest("LocationRepository deletes locations") {
    val locations = LocationRepository.makePostgres[IO](sessionPool)

    for
      paris <- locations.create(sampleLocation(22, "Paris", LocationType.City, country = Some("France"), city = Some("Paris")))
      deleted <- locations.delete(paris.id)
      deletedById <- locations.findById(paris.id)
    yield
      assertEquals(deleted, true)
      assertEquals(deletedById, None)
  }
