package gotrip.service.provider

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.provider.*
import gotrip.repository.provider.ProviderRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

final class ProviderServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "ProviderService" should {
    "delegate search to repository and return providers" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.search.expects(searchParams).returning(IO.pure(List(provider)))

      service.search(searchParams).unsafeRunSync() shouldBe List(provider)
    }

    "delegate findById to repository" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(Some(provider)))

      service.findById(providerId).unsafeRunSync() shouldBe Some(provider)
    }

    "create a provider when name is available" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.nameExists.expects(providerCreate.name, None).returning(IO.pure(false))
      repository.create.expects(where { (created: Provider) =>
        created.id.value != new UUID(0L, 0L) &&
        created.name == providerCreate.name &&
        created.`type` == providerCreate.`type` &&
        created.website == providerCreate.website &&
        created.support_contact == providerCreate.support_contact
      }).returning(IO.pure(provider))

      service.create(providerCreate).unsafeRunSync() shouldBe Right(provider)
    }

    "reject create when provider name is duplicated" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.nameExists.expects(providerCreate.name, None).returning(IO.pure(true))

      service.create(providerCreate).unsafeRunSync() shouldBe
        Left(ProviderServiceError.DuplicateProviderName(providerCreate.name))
    }

    "return not found when updating a missing provider" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(None))

      service.update(providerId, providerUpdate).unsafeRunSync() shouldBe
        Left(ProviderServiceError.ProviderNotFound(providerId))
    }

    "reject update when new name is duplicated" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(Some(provider)))
      repository.nameExists.expects(updatedName, Some(providerId)).returning(IO.pure(true))

      service.update(providerId, providerUpdate).unsafeRunSync() shouldBe
        Left(ProviderServiceError.DuplicateProviderName(updatedName))
    }

    "update a provider when new name is available" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(Some(provider)))
      repository.nameExists.expects(updatedName, Some(providerId)).returning(IO.pure(false))
      repository.update.expects(providerId, providerUpdate).returning(IO.pure(Some(updatedProvider)))

      service.update(providerId, providerUpdate).unsafeRunSync() shouldBe Right(updatedProvider)
    }

    "reject delete when provider has additional services" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(Some(provider)))
      repository.hasAdditionalServices.expects(providerId).returning(IO.pure(true))

      service.delete(providerId).unsafeRunSync() shouldBe
        Left(ProviderServiceError.ProviderInUse(providerId))
    }

    "delete an existing provider that is not in use" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(Some(provider)))
      repository.hasAdditionalServices.expects(providerId).returning(IO.pure(false))
      repository.delete.expects(providerId).returning(IO.pure(true))

      service.delete(providerId).unsafeRunSync() shouldBe Right(())
    }

    "return not found when deleting a missing provider" in {
      val repository = mock[ProviderRepository[IO]]
      val service = ProviderService[IO](repository)

      repository.findById.expects(providerId).returning(IO.pure(None))

      service.delete(providerId).unsafeRunSync() shouldBe
        Left(ProviderServiceError.ProviderNotFound(providerId))
    }
  }

  private val providerId = ProviderId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
  private val providerName = ProviderName("SkyWays")
  private val updatedName = ProviderName("SkyWays Premium")

  private val searchParams = ProviderSearchParams(
    `type` = Some(ProviderType.Airline),
    query = Some("sky")
  )

  private val providerCreate = ProviderCreate(
    name = providerName,
    `type` = ProviderType.Airline,
    website = Some("https://skyways.example.com"),
    support_contact = Some("support@skyways.example.com")
  )

  private val provider = Provider(
    id = providerId,
    name = providerName,
    `type` = ProviderType.Airline,
    website = providerCreate.website,
    support_contact = providerCreate.support_contact
  )

  private val providerUpdate = ProviderUpdate(
    name = Some(updatedName),
    `type` = Some(ProviderType.BookingPlatform),
    website = Some("https://premium.skyways.example.com")
  )

  private val updatedProvider = provider.copy(
    name = updatedName,
    `type` = ProviderType.BookingPlatform,
    website = Some("https://premium.skyways.example.com")
  )
