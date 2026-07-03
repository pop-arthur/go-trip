package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.location.*
import gotrip.repository.location.LocationRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class LocationRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "LocationRepository" should {
    "create, find, search, update, and delete locations" in {
      val locations = LocationRepository.makePostgres[IO](sessionPool)
      val hanoi = locations.create(sampleLocation(21, "Noi Bai International Airport", LocationType.Airport)).unsafeRunSync()
      val paris = locations.create(sampleLocation(22, "Paris", LocationType.City, country = Some("France"), city = Some("Paris"))).unsafeRunSync()

      locations.findById(hanoi.id).unsafeRunSync() shouldBe Some(hanoi)
      locations.findAll().unsafeRunSync().map(_.id) shouldBe List(hanoi.id, paris.id)
      locations.search(LocationSearchParams(`type` = Some(LocationType.Airport), country = Some("Vietnam"), city = Some("Hanoi"), query = Some("noi"))).unsafeRunSync() shouldBe List(hanoi)

      val updated = locations.update(hanoi.id, LocationUpdate(name = Some(LocationName("Noi Bai Airport")), address = Some(LocationAddress(None)))).unsafeRunSync()
      updated.map(_.name) shouldBe Some(LocationName("Noi Bai Airport"))
      updated.flatMap(_.address.value) shouldBe None

      locations.delete(paris.id).unsafeRunSync() shouldBe true
      locations.findById(paris.id).unsafeRunSync() shouldBe None
    }
  }
