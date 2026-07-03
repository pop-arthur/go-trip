package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.location.LocationType
import gotrip.domain.provider.*
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ProviderRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "ProviderRepository" should {
    "create, search, check names, update, delete, and detect additional services" in {
      val providers = ProviderRepository.makePostgres[IO](sessionPool)
      val locations = LocationRepository.makePostgres[IO](sessionPool)
      val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

      val provider = providers.create(sampleProvider(23, "Vietnam Airlines", ProviderType.Airline)).unsafeRunSync()
      val otherProvider = providers.create(sampleProvider(24, "City Hotel", ProviderType.Hotel)).unsafeRunSync()
      val location = locations.create(sampleLocation(25, "Noi Bai", LocationType.Airport)).unsafeRunSync()

      providers.findById(provider.id).unsafeRunSync() shouldBe Some(provider)
      providers.search(ProviderSearchParams(`type` = Some(ProviderType.Airline), query = Some("vietnam"))).unsafeRunSync() shouldBe List(provider)
      providers.nameExists(ProviderName("VIETNAM AIRLINES")).unsafeRunSync() shouldBe true
      providers.nameExists(provider.name, Some(provider.id)).unsafeRunSync() shouldBe false
      providers.update(otherProvider.id, ProviderUpdate(name = Some(ProviderName("Boutique Hotel")))).unsafeRunSync().map(_.name) shouldBe Some(ProviderName("Boutique Hotel"))

      val service = services.create(sampleService(26, provider.id, location.id)).unsafeRunSync()
      providers.hasAdditionalServices(provider.id).unsafeRunSync() shouldBe true
      services.delete(service.id).unsafeRunSync() shouldBe true
      providers.delete(provider.id).unsafeRunSync() shouldBe true
    }
  }
