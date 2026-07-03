package gotrip.repository

import cats.effect.IO
import gotrip.domain.additionalservice.*
import gotrip.domain.location.LocationType
import gotrip.domain.provider.ProviderType
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.location.LocationRepository
import gotrip.repository.provider.ProviderRepository

final class AdditionalServiceRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("AdditionalServiceRepository creates, finds, and searches services") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(31, "Airport Services", ProviderType.Other))
      location <- locations.create(sampleLocation(32, "Noi Bai", LocationType.Airport))
      service <- services.create(sampleService(33, provider.id, location.id))
      byId <- services.findById(service.id)
      search <- services.search(AdditionalServiceSearchParams(serviceType = Some(ServiceType.Lounge), providerId = Some(provider.id), locationId = Some(location.id)))
    yield
      assertEquals(byId, Some(service))
      assertEquals(search, List(service))
  }

  repositoryTest("AdditionalServiceRepository checks provider and location existence") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(34, "Airport Services", ProviderType.Other))
      location <- locations.create(sampleLocation(35, "Noi Bai", LocationType.Airport))
      providerExists <- services.providerExists(provider.id)
      locationExists <- services.locationExists(location.id)
    yield
      assertEquals(providerExists, true)
      assertEquals(locationExists, true)
  }

  repositoryTest("AdditionalServiceRepository updates services") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(36, "Airport Services", ProviderType.Other))
      location <- locations.create(sampleLocation(37, "Noi Bai", LocationType.Airport))
      service <- services.create(sampleService(38, provider.id, location.id))
      updated <- services.update(service.id, AdditionalServiceUpdate(title = Some(ServiceTitle("Business Lounge")), is_active = Some(false)))
    yield
      assertEquals(updated.map(_.title), Some(ServiceTitle("Business Lounge")))
      assertEquals(updated.map(_.is_active), Some(false))
  }

  repositoryTest("AdditionalServiceRepository deletes services") {
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val services = AdditionalServiceRepository.makePostgres[IO](sessionPool)

    for
      provider <- providers.create(sampleProvider(39, "Airport Services", ProviderType.Other))
      location <- locations.create(sampleLocation(40, "Noi Bai", LocationType.Airport))
      service <- services.create(sampleService(41, provider.id, location.id))
      deleted <- services.delete(service.id)
      byId <- services.findById(service.id)
    yield
      assertEquals(deleted, true)
      assertEquals(byId, None)
  }
