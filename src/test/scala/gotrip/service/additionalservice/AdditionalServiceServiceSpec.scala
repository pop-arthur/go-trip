package gotrip.service.additionalservice

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

final class AdditionalServiceServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport:

  "AdditionalServiceService" should {
    "delegate search to repository and return services" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.search.expects(searchParams).returning(IO.pure(List(additionalService)))

      service.search(searchParams).unsafeRunSync() shouldBe List(additionalService)
    }

    "delegate findById to repository" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.findById.expects(serviceId).returning(IO.pure(Some(additionalService)))

      service.findById(serviceId).unsafeRunSync() shouldBe Some(additionalService)
    }

    "create a service with default active flag" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)

      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(true))
      expectGeneratedId(generatedData, serviceId.value)
      repository.create.expects(additionalService).returning(IO.pure(additionalService))

      service.create(additionalServiceCreate).unsafeRunSync() shouldBe Right(additionalService)
    }

    "reject create when provider does not exist" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.providerExists.expects(providerId).returning(IO.pure(false))

      service.create(additionalServiceCreate).unsafeRunSync() shouldBe
        Left(AdditionalServiceServiceError.ProviderNotFound(providerId))
    }

    "reject create when location does not exist" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(false))

      service.create(additionalServiceCreate).unsafeRunSync() shouldBe
        Left(AdditionalServiceServiceError.LocationNotFound(locationId))
    }

    "return not found when updating a missing service" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.findById.expects(serviceId).returning(IO.pure(None))

      service.update(serviceId, additionalServiceUpdate).unsafeRunSync() shouldBe
        Left(AdditionalServiceServiceError.AdditionalServiceNotFound(serviceId))
    }

    "check provider and location before updating" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.findById.expects(serviceId).returning(IO.pure(Some(additionalService)))
      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(true))
      repository.update.expects(serviceId, additionalServiceUpdate).returning(IO.pure(Some(updatedService)))

      service.update(serviceId, additionalServiceUpdate).unsafeRunSync() shouldBe Right(updatedService)
    }

    "reject update when provider does not exist" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.findById.expects(serviceId).returning(IO.pure(Some(additionalService)))
      repository.providerExists.expects(providerId).returning(IO.pure(false))

      service.update(serviceId, additionalServiceUpdate).unsafeRunSync() shouldBe
        Left(AdditionalServiceServiceError.ProviderNotFound(providerId))
    }

    "delete an existing service" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.delete.expects(serviceId).returning(IO.pure(true))

      service.delete(serviceId).unsafeRunSync() shouldBe Right(())
    }

    "return not found when deleting a missing service" in {
      val repository = mock[AdditionalServiceRepository[IO]]
      val service = AdditionalServiceService[IO](repository)

      repository.delete.expects(serviceId).returning(IO.pure(false))

      service.delete(serviceId).unsafeRunSync() shouldBe
        Left(AdditionalServiceServiceError.AdditionalServiceNotFound(serviceId))
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: AdditionalServiceRepository[IO],
    generatedData: GeneratedData[IO]
  ): AdditionalServiceService[IO] =
    given GeneratedData[IO] = generatedData
    AdditionalServiceService[IO](repository)

  private val serviceId = ServiceId(uuid("000000000001"))
  private val providerId = ProviderId(uuid("000000000002"))
  private val locationId = LocationId(uuid("000000000003"))

  private val searchParams = AdditionalServiceSearchParams(
    serviceType = Some(ServiceType.Lounge),
    locationId = Some(locationId),
    providerId = Some(providerId)
  )

  private val additionalServiceCreate = AdditionalServiceCreate(
    title = ServiceTitle("Airport lounge"),
    description = Some("Quiet lounge access"),
    service_type = ServiceType.Lounge,
    provider_id = Some(providerId),
    location_id = Some(locationId),
    price_amount = Some(35.0),
    price_currency = Some("USD")
  )

  private val additionalService = AdditionalService(
    id = serviceId,
    title = additionalServiceCreate.title,
    description = additionalServiceCreate.description,
    service_type = additionalServiceCreate.service_type,
    provider_id = additionalServiceCreate.provider_id,
    location_id = additionalServiceCreate.location_id,
    price_amount = additionalServiceCreate.price_amount,
    price_currency = additionalServiceCreate.price_currency,
    is_active = true
  )

  private val additionalServiceUpdate = AdditionalServiceUpdate(
    title = Some(ServiceTitle("Premium lounge")),
    provider_id = Some(providerId),
    location_id = Some(locationId),
    is_active = Some(false)
  )

  private val updatedService = additionalService.copy(
    title = ServiceTitle("Premium lounge"),
    is_active = false
  )
