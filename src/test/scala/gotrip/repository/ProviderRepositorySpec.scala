package gotrip.repository

import cats.effect.IO
import gotrip.domain.location.LocationType
import gotrip.domain.provider.*
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository

final class ProviderRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("ProviderRepository creates, finds, and searches providers") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(23, "Vietnam Airlines", ProviderType.Airline))
      _ <- providers.create(sampleProvider(24, "City Hotel", ProviderType.Hotel))
      byId <- providers.findById(provider.id)
      search <- providers.search(ProviderSearchParams(`type` = Some(ProviderType.Airline), query = Some("vietnam")))
    yield
      assertEquals(byId, Some(provider))
      assertEquals(search, List(provider))
  }

  repositoryTest("ProviderRepository checks names with optional exclusion") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(23, "Vietnam Airlines", ProviderType.Airline))
      nameExists <- providers.nameExists(ProviderName("VIETNAM AIRLINES"))
      nameExistsWithExclude <- providers.nameExists(provider.name, Some(provider.id))
    yield
      assertEquals(nameExists, true)
      assertEquals(nameExistsWithExclude, false)
  }

  repositoryTest("ProviderRepository updates providers") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(24, "City Hotel", ProviderType.Hotel))
      updated <- providers.update(provider.id, ProviderUpdate(name = Some(ProviderName("Boutique Hotel"))))
    yield assertEquals(updated.map(_.name), Some(ProviderName("Boutique Hotel")))
  }

  repositoryTest("ProviderRepository detects additional services") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(23, "Vietnam Airlines", ProviderType.Airline))
      location <- locations.create(sampleLocation(25, "Noi Bai", LocationType.Airport))
      _ <- services.create(sampleService(26, provider.id, location.id))
      hasServices <- providers.hasAdditionalServices(provider.id)
    yield assertEquals(hasServices, true)
  }

  repositoryTest("ProviderRepository deletes providers") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(23, "Vietnam Airlines", ProviderType.Airline))
      providerDeleted <- providers.delete(provider.id)
      byId <- providers.findById(provider.id)
    yield
      assertEquals(providerDeleted, true)
      assertEquals(byId, None)
  }
