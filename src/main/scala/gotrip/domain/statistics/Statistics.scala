package gotrip.domain.statistics

import gotrip.domain.trip.Trip

case class CountriesStatisticsResponse(
  period: String,
  countries_count: Int,
  countries: List[String]
)

case class SpendingStatisticsResponse(
  total_amount: Double,
  currency: String,
  items: List[SpendingItem]
)

case class SpendingItem(
  trip_id: Long,
  trip_title: String,
  amount: Double
)

case class UpcomingTripsResponse(
  trips: List[UpcomingTrip]
)

case class UpcomingTrip(
  trip: Trip,
  duration_days: Int
)

case class TripDurationsResponse(
  items: List[TripDuration]
)

case class TripDuration(
  trip_id: Long,
  trip_title: String,
  duration_days: Int,
  duration_hours: Int
)

case class CountriesStatisticsParams(
  period: Option[String],
  from: Option[java.time.LocalDate],
  to: Option[java.time.LocalDate]
)

case class SpendingStatisticsParams(
  from: Option[java.time.LocalDate],
  to: Option[java.time.LocalDate],
  currency: Option[String]
)

case class TripDurationsParams(
  from: Option[java.time.LocalDate],
  to: Option[java.time.LocalDate]
)