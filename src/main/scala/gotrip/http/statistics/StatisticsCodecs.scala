package gotrip.http.statistics

import gotrip.domain.statistics._
import gotrip.http.ApiError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.derived

object StatisticsCodecs {
  import gotrip.http.trip.TripCodecs.given

  given Encoder[ApiError] = deriveEncoder
  given Decoder[ApiError] = deriveDecoder
  given Schema[ApiError] = derived

  given Encoder[CountriesStatisticsResponse] = deriveEncoder
  given Decoder[CountriesStatisticsResponse] = deriveDecoder
  given Schema[CountriesStatisticsResponse] = derived

  given Encoder[SpendingItem] = deriveEncoder
  given Decoder[SpendingItem] = deriveDecoder
  given Schema[SpendingItem] = derived

  given Encoder[SpendingStatisticsResponse] = deriveEncoder
  given Decoder[SpendingStatisticsResponse] = deriveDecoder
  given Schema[SpendingStatisticsResponse] = derived

  given Encoder[UpcomingTrip] = deriveEncoder
  given Decoder[UpcomingTrip] = deriveDecoder
  given Schema[UpcomingTrip] = derived

  given Encoder[UpcomingTripsResponse] = deriveEncoder
  given Decoder[UpcomingTripsResponse] = deriveDecoder
  given Schema[UpcomingTripsResponse] = derived

  given Encoder[TripDuration] = deriveEncoder
  given Decoder[TripDuration] = deriveDecoder
  given Schema[TripDuration] = derived

  given Encoder[TripDurationsResponse] = deriveEncoder
  given Decoder[TripDurationsResponse] = deriveDecoder
  given Schema[TripDurationsResponse] = derived
}