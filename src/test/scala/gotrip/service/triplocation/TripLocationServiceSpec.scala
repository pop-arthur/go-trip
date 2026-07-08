package gotrip.service.triplocation

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.OffsetDateTime
import java.util.UUID

final class TripLocationServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport:

  "TripLocationService" should {
    "list locations for an owned trip" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.listByTrip.expects(tripId).returning(IO.pure(List(tripLocation)))

      service.listByTrip(userId, tripId).unsafeRunSync() shouldBe Right(List(tripLocation))
    }

    "return trip not found when listing an inaccessible trip" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(false))

      service.listByTrip(userId, tripId).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.TripNotFound(tripId))
    }

    "create a location with explicit visit order" in {
      val repository = mock[TripLocationRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(true))
      repository.visitOrderExists.expects(tripId, explicitVisitOrder, None).returning(IO.pure(false))
      expectGeneratedId(generatedData, tripLocationId.value)
      repository.create.expects(tripLocation).returning(IO.pure(tripLocation))

      service.create(userId, tripId, tripLocationCreate).unsafeRunSync() shouldBe Right(tripLocation)
    }

    "create a location with generated next visit order" in {
      val repository = mock[TripLocationRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(true))
      repository.nextVisitOrder.expects(tripId).returning(IO.pure(generatedVisitOrder))
      repository.visitOrderExists.expects(tripId, generatedVisitOrder, None).returning(IO.pure(false))
      expectGeneratedId(generatedData, tripLocationId.value)
      repository.create.expects(generatedTripLocation).returning(IO.pure(generatedTripLocation))

      service.create(userId, tripId, tripLocationCreate.copy(visit_order = None)).unsafeRunSync() shouldBe
        Right(generatedTripLocation)
    }

    "reject create when arrival date is after departure date" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      service.create(userId, tripId, invalidTripLocationCreate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.InvalidDateRange)
    }

    "reject create when trip does not belong to user" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(false))

      service.create(userId, tripId, tripLocationCreate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.TripNotFound(tripId))
    }

    "reject create when location does not exist" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(false))

      service.create(userId, tripId, tripLocationCreate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.LocationNotFound(locationId))
    }

    "reject create when visit order is already used" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.locationExists.expects(locationId).returning(IO.pure(true))
      repository.visitOrderExists.expects(tripId, explicitVisitOrder, None).returning(IO.pure(true))

      service.create(userId, tripId, tripLocationCreate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.DuplicateVisitOrder(explicitVisitOrder))
    }

    "return not found when updating a missing trip location" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.findInTrip.expects(tripId, tripLocationId).returning(IO.pure(None))

      service.update(userId, tripId, tripLocationId, tripLocationUpdate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.TripLocationNotFound(tripLocationId))
    }

    "reject update when merged date range is invalid" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.findInTrip.expects(tripId, tripLocationId).returning(IO.pure(Some(tripLocation)))

      service.update(userId, tripId, tripLocationId, invalidTripLocationUpdate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.InvalidDateRange)
    }

    "reject update when visit order is already used by another trip location" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.findInTrip.expects(tripId, tripLocationId).returning(IO.pure(Some(tripLocation)))
      repository.visitOrderExists.expects(tripId, generatedVisitOrder, Some(tripLocationId)).returning(IO.pure(true))

      service.update(userId, tripId, tripLocationId, tripLocationUpdate).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.DuplicateVisitOrder(generatedVisitOrder))
    }

    "update an existing trip location" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.findInTrip.expects(tripId, tripLocationId).returning(IO.pure(Some(tripLocation)))
      repository.visitOrderExists.expects(tripId, generatedVisitOrder, Some(tripLocationId)).returning(IO.pure(false))
      repository.update.expects(tripId, tripLocationId, tripLocationUpdate).returning(IO.pure(Some(updatedTripLocation)))

      service.update(userId, tripId, tripLocationId, tripLocationUpdate).unsafeRunSync() shouldBe
        Right(updatedTripLocation)
    }

    "delete an existing trip location" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.delete.expects(tripId, tripLocationId).returning(IO.pure(true))

      service.delete(userId, tripId, tripLocationId).unsafeRunSync() shouldBe Right(())
    }

    "return not found when deleting a missing trip location" in {
      val repository = mock[TripLocationRepository[IO]]
      val service = TripLocationService[IO](repository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.delete.expects(tripId, tripLocationId).returning(IO.pure(false))

      service.delete(userId, tripId, tripLocationId).unsafeRunSync() shouldBe
        Left(TripLocationServiceError.TripLocationNotFound(tripLocationId))
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: TripLocationRepository[IO],
    generatedData: GeneratedData[IO]
  ): TripLocationService[IO] =
    given GeneratedData[IO] = generatedData
    TripLocationService[IO](repository)

  private val userId = UserId(uuid("000000000001"))
  private val tripId = TripId(uuid("000000000010"))
  private val locationId = LocationId(uuid("000000000020"))
  private val tripLocationId = TripLocationId(uuid("000000000030"))
  private val arrivalDate = OffsetDateTime.parse("2026-06-10T12:00:00Z")
  private val departureDate = OffsetDateTime.parse("2026-06-12T12:00:00Z")
  private val explicitVisitOrder = VisitOrder(2)
  private val generatedVisitOrder = VisitOrder(3)

  private val tripLocationCreate = TripLocationCreate(
    location_id = locationId,
    visit_order = Some(explicitVisitOrder),
    arrival_date = TripLocationArrivalDate(Some(arrivalDate)),
    departure_date = TripLocationDepartureDate(Some(departureDate))
  )

  private val invalidTripLocationCreate = tripLocationCreate.copy(
    arrival_date = TripLocationArrivalDate(Some(departureDate.plusDays(1)))
  )

  private val tripLocation = TripLocation(
    id = tripLocationId,
    trip_id = tripId,
    location_id = locationId,
    visit_order = explicitVisitOrder,
    arrival_date = tripLocationCreate.arrival_date,
    departure_date = tripLocationCreate.departure_date
  )

  private val generatedTripLocation = tripLocation.copy(visit_order = generatedVisitOrder)

  private val tripLocationUpdate = TripLocationUpdate(
    visit_order = Some(generatedVisitOrder),
    departure_date = Some(TripLocationDepartureDate(Some(departureDate.plusDays(1))))
  )

  private val invalidTripLocationUpdate = TripLocationUpdate(
    arrival_date = Some(TripLocationArrivalDate(Some(departureDate.plusDays(1))))
  )

  private val updatedTripLocation = tripLocation.copy(
    visit_order = generatedVisitOrder,
    departure_date = TripLocationDepartureDate(Some(departureDate.plusDays(1)))
  )
