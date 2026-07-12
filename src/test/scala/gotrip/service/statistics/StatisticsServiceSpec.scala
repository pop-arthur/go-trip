package gotrip.service.statistics

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.statistics._
import gotrip.domain.trip._
import gotrip.domain.user.UserId
import gotrip.repository.statistics.StatisticsRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDate, Instant}
import java.util.UUID

final class StatisticsServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "StatisticsService" should {
    "get countries statistics" in {
      val repo = mock[StatisticsRepository[IO]]
      val service = new StatisticsService[IO](repo)
      val userId = UserId(UUID.randomUUID())
      val params = CountriesStatisticsParams(period = Some("ALL_TIME"), from = None, to = None)
      val response = CountriesStatisticsResponse("ALL_TIME", 2, List("Country1", "Country2"))

      repo.getCountriesStatistics.expects(userId, params).returning(IO.pure(response))

      service.getCountriesStatistics(userId, params).unsafeRunSync() shouldBe response
    }

    "get spending statistics" in {
      val repo = mock[StatisticsRepository[IO]]
      val service = new StatisticsService[IO](repo)
      val userId = UserId(UUID.randomUUID())
      val params = SpendingStatisticsParams(from = None, to = None, currency = Some("USD"))
      val items = List(SpendingItem("trip-1", "Trip 1", 100.0), SpendingItem("trip-2", "Trip 2", 200.0))
      val response = SpendingStatisticsResponse(300.0, "USD", items)

      repo.getSpendingStatistics.expects(userId, params).returning(IO.pure(response))

      service.getSpendingStatistics(userId, params).unsafeRunSync() shouldBe response
    }

    "get upcoming trips statistics" in {
      val repo = mock[StatisticsRepository[IO]]
      val service = new StatisticsService[IO](repo)
      val userId = UserId(UUID.randomUUID())
      val trip1 = Trip(
        id = TripId(UUID.randomUUID()),
        user_id = userId,
        title = TripTitle("Trip 1"),
        start_date = TripStartDate(Some(LocalDate.now().plusDays(1))),
        end_date = TripEndDate(Some(LocalDate.now().plusDays(3))),
        status = TripStatus.Planned,
        created_at = Instant.now(),
        updated_at = Instant.now()
      )
      val response = UpcomingTripsResponse(List(UpcomingTrip(trip1, 2)))

      repo.getUpcomingTripsStatistics.expects(userId).returning(IO.pure(response))

      service.getUpcomingTripsStatistics(userId).unsafeRunSync() shouldBe response
    }

    "get trip durations statistics" in {
      val repo = mock[StatisticsRepository[IO]]
      val service = new StatisticsService[IO](repo)
      val userId = UserId(UUID.randomUUID())
      val params = TripDurationsParams(from = None, to = None)
      val items = List(TripDuration("trip-1", "Trip 1", 5, 120), TripDuration("trip-2", "Trip 2", 3, 72))
      val response = TripDurationsResponse(items)

      repo.getTripDurationsStatistics.expects(userId, params).returning(IO.pure(response))

      service.getTripDurationsStatistics(userId, params).unsafeRunSync() shouldBe response
    }
  }
}