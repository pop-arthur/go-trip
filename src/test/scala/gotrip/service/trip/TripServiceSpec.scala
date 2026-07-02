package gotrip.service.trip

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.trip.TripRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, LocalDate}
import java.util.UUID

final class TripServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "TripService" should {
    "delegate listByUser to repository and return trips" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.listByUser.expects(userId, searchParams).returning(IO.pure(List(trip)))

      service.listByUser(userId, searchParams).unsafeRunSync() shouldBe List(trip)
    }

    "find an owned trip" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.findByUser.expects(userId, tripId).returning(IO.pure(Some(trip)))

      service.findByUser(userId, tripId).unsafeRunSync() shouldBe Right(trip)
    }

    "return not found when finding an inaccessible trip" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.findByUser.expects(userId, tripId).returning(IO.pure(None))

      service.findByUser(userId, tripId).unsafeRunSync() shouldBe Left(TripServiceError.TripNotFound(tripId))
    }

    "create a valid trip for a user" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.create.expects(where { (created: Trip) =>
        created.id.value != new UUID(0L, 0L) &&
        created.user_id == userId &&
        created.title == tripCreate.title &&
        created.start_date == tripCreate.start_date &&
        created.end_date == tripCreate.end_date &&
        created.status == TripStatus.Planned
      }).returning(IO.pure(trip))

      service.create(userId, tripCreate).unsafeRunSync() shouldBe Right(trip)
    }

    "reject create when start date is after end date" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      service.create(userId, invalidTripCreate).unsafeRunSync() shouldBe Left(TripServiceError.InvalidDateRange)
    }

    "return not found when updating a trip that is not owned by the user" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.findByUser.expects(userId, tripId).returning(IO.pure(None))

      service.update(userId, tripId, TripUpdate(title = Some(TripTitle("Updated")))).unsafeRunSync() shouldBe
        Left(TripServiceError.TripNotFound(tripId))
    }

    "update an owned trip with merged fields" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.findByUser.expects(userId, tripId).returning(IO.pure(Some(trip)))
      repository.update.expects(where { (updated: Trip) =>
        updated.id == tripId &&
        updated.title == TripTitle("Updated") &&
        updated.start_date == trip.start_date &&
        updated.end_date == TripEndDate(Some(LocalDate.parse("2026-06-25"))) &&
        updated.status == TripStatus.Completed &&
        updated.created_at == trip.created_at &&
        updated.updated_at != trip.updated_at
      }).returning(IO.pure(Some(updatedTrip)))

      service.update(userId, tripId, tripUpdate).unsafeRunSync() shouldBe Right(updatedTrip)
    }

    "reject update when merged start date is after end date" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.findByUser.expects(userId, tripId).returning(IO.pure(Some(trip)))

      service.update(userId, tripId, invalidTripUpdate).unsafeRunSync() shouldBe
        Left(TripServiceError.InvalidDateRange)
    }

    "delete an owned trip" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.delete.expects(userId, tripId).returning(IO.pure(true))

      service.delete(userId, tripId).unsafeRunSync() shouldBe Right(())
    }

    "return not found when deleting an inaccessible trip" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.delete.expects(userId, tripId).returning(IO.pure(false))

      service.delete(userId, tripId).unsafeRunSync() shouldBe Left(TripServiceError.TripNotFound(tripId))
    }
  }

  private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
  private val tripId = TripId(UUID.fromString("00000000-0000-0000-0000-000000000010"))

  private val tripCreate = TripCreate(
    title = TripTitle("Vietnam 2026"),
    start_date = TripStartDate(Some(LocalDate.parse("2026-06-10"))),
    end_date = TripEndDate(Some(LocalDate.parse("2026-06-20"))),
    status = None
  )

  private val invalidTripCreate = tripCreate.copy(
    start_date = TripStartDate(Some(LocalDate.parse("2026-06-21")))
  )

  private val searchParams = TripSearchParams(
    status = Some(TripStatus.Planned),
    fromDate = Some(LocalDate.parse("2026-06-01")),
    toDate = Some(LocalDate.parse("2026-06-30"))
  )

  private val tripUpdate = TripUpdate(
    title = Some(TripTitle("Updated")),
    end_date = Some(TripEndDate(Some(LocalDate.parse("2026-06-25")))),
    status = Some(TripStatus.Completed)
  )

  private val invalidTripUpdate = TripUpdate(
    start_date = Some(TripStartDate(Some(LocalDate.parse("2026-06-30"))))
  )

  private val trip = Trip(
    id = tripId,
    user_id = userId,
    title = TripTitle("Vietnam 2026"),
    start_date = TripStartDate(Some(LocalDate.parse("2026-06-10"))),
    end_date = TripEndDate(Some(LocalDate.parse("2026-06-20"))),
    status = TripStatus.Planned,
    created_at = Instant.parse("2026-06-01T10:00:00Z"),
    updated_at = Instant.parse("2026-06-01T10:00:00Z")
  )

  private val updatedTrip = trip.copy(
    title = TripTitle("Updated"),
    end_date = TripEndDate(Some(LocalDate.parse("2026-06-25"))),
    status = TripStatus.Completed,
    updated_at = Instant.parse("2026-06-01T10:05:00Z")
  )
