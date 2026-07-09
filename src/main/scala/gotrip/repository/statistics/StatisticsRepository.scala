package gotrip.repository.statistics

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap._
import cats.syntax.functor._
import gotrip.domain.statistics._
import gotrip.domain.trip._
import gotrip.domain.user._
import gotrip.repository.SkunkCodecs.tripStatus
import skunk._
import skunk.codec.all._
import skunk.implicits._
import java.time.LocalDate

trait StatisticsRepository[F[_]] {
  def getCountriesStatistics(userId: UserId, params: CountriesStatisticsParams): F[CountriesStatisticsResponse]
  def getSpendingStatistics(userId: UserId, params: SpendingStatisticsParams): F[SpendingStatisticsResponse]
  def getUpcomingTripsStatistics(userId: UserId): F[UpcomingTripsResponse]
  def getTripDurationsStatistics(userId: UserId, params: TripDurationsParams): F[TripDurationsResponse]
}

object StatisticsRepository {
  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): StatisticsRepository[F] =
    new PostgresStatisticsRepository(sessionPool)
}

final class PostgresStatisticsRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends StatisticsRepository[F] {

  override def getCountriesStatistics(userId: UserId, params: CountriesStatisticsParams): F[CountriesStatisticsResponse] = {
    val period = params.period.getOrElse("ALL_TIME")
    val query = sql"""
      SELECT DISTINCT l.country
      FROM trips t
      JOIN trip_locations tl ON tl.trip_id = t.id
      JOIN locations l ON l.id = tl.location_id
      WHERE t.user_id = $uuid
        AND l.type = 'COUNTRY'
        AND l.country IS NOT NULL
        AND (t.start_date >= COALESCE(${date.opt}, t.start_date))
        AND (t.end_date <= COALESCE(${date.opt}, t.end_date))
      ORDER BY l.country
    """.query(text)
    sessionPool.use { session =>
      session.prepare(query).flatMap { q =>
        q.stream((userId.value, params.from, params.to), 64).compile.toList.map { countries =>
          CountriesStatisticsResponse(period, countries.size, countries)
        }
      }
    }
  }

  override def getSpendingStatistics(userId: UserId, params: SpendingStatisticsParams): F[SpendingStatisticsResponse] = {
    val currency = params.currency.getOrElse("USD")
    val query = sql"""
      SELECT 
        t.id, 
        t.title,
        COALESCE(SUM(o.price_amount), 0.0) AS total
      FROM trips t
      LEFT JOIN orders o ON o.trip_id = t.id 
        AND o.user_id = t.user_id 
        AND o.price_amount IS NOT NULL
        AND o.price_currency = $text
        AND o.status NOT IN ('CANCELLED', 'REFUNDED')
      WHERE t.user_id = $uuid
        AND (t.start_date >= COALESCE(${date.opt}, t.start_date))
        AND (t.end_date <= COALESCE(${date.opt}, t.end_date))
      GROUP BY t.id, t.title
      ORDER BY t.id
    """.query(int8 ~ text ~ float8)
    sessionPool.use { session =>
      session.prepare(query).flatMap { q =>
        q.stream((currency, userId.value, params.from, params.to), 64).compile.toList.map { rows =>
          val items = rows.map { case id ~ title ~ amount =>
            SpendingItem(id, title, amount)
          }
          val total = items.map(_.amount).sum
          SpendingStatisticsResponse(total, currency, items)
        }
      }
    }
  }

  override def getUpcomingTripsStatistics(userId: UserId): F[UpcomingTripsResponse] = {
    val today = LocalDate.now()
    val query = sql"""
      SELECT id, user_id, title, start_date, end_date, status, created_at, updated_at
      FROM trips
      WHERE user_id = $uuid
        AND start_date >= $date
        AND status IN ('PLANNED', 'ACTIVE')
      ORDER BY start_date
    """.query(tripDecoder)
    sessionPool.use { session =>
      session.prepare(query).flatMap { q =>
        q.stream((userId.value, today), 64).compile.toList.map { trips =>
          val upcoming = trips.map { trip =>
            val duration = trip.end_date.value match {
              case Some(end) =>
                val startDate = trip.start_date.value.getOrElse(today)
                val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, end).toInt
                days
              case None => 0
            }
            UpcomingTrip(trip, duration)
          }
          UpcomingTripsResponse(upcoming)
        }
      }
    }
  }

  override def getTripDurationsStatistics(userId: UserId, params: TripDurationsParams): F[TripDurationsResponse] = {
    val query = sql"""
      SELECT id, title, start_date, end_date
      FROM trips
      WHERE user_id = $uuid
        AND start_date IS NOT NULL
        AND end_date IS NOT NULL
        AND (start_date >= COALESCE(${date.opt}, start_date))
        AND (end_date <= COALESCE(${date.opt}, end_date))
      ORDER BY start_date
    """.query(int8 ~ text ~ date.opt ~ date.opt)
    sessionPool.use { session =>
      session.prepare(query).flatMap { q =>
        q.stream((userId.value, params.from, params.to), 64).compile.toList.map { rows =>
          val items = rows.map { case id ~ title ~ startOpt ~ endOpt =>
            val startDate = startOpt.getOrElse(LocalDate.now())
            val endDate = endOpt.getOrElse(startDate)
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt
            val hours = days * 24
            TripDuration(id, title, days, hours)
          }
          TripDurationsResponse(items)
        }
      }
    }
  }

  private val tripDecoder: Decoder[Trip] =
    (uuid ~ uuid ~ text ~ date.opt ~ date.opt ~ tripStatus ~ timestamptz ~ timestamptz)
      .map { case id ~ uid ~ title ~ start ~ end ~ status ~ created ~ updated =>
        Trip(
          id = TripId(id),
          user_id = UserId(uid),
          title = TripTitle(title),
          start_date = TripStartDate(start),
          end_date = TripEndDate(end),
          status = status,
          created_at = created.toInstant,
          updated_at = updated.toInstant
        )
      }
}