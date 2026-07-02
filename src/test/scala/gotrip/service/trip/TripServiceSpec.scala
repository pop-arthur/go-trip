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
    "create a valid trip for a user" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.create.expects(*).returning(IO.pure(trip))

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

    "delete an owned trip" in {
      val repository = mock[TripRepository[IO]]
      val service = TripService[IO](repository)

      repository.delete.expects(userId, tripId).returning(IO.pure(true))

      service.delete(userId, tripId).unsafeRunSync() shouldBe Right(())
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
