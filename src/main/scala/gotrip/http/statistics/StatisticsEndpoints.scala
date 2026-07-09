package gotrip.http.statistics

import gotrip.domain.statistics._
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.tapir._
import sttp.tapir.json.circe._

import java.time.LocalDate

object StatisticsEndpoints {
  import StatisticsCodecs.given

  type ErrorResponse = HttpError

  val getCountriesStatistics: Endpoint[String, (Option[String], Option[LocalDate], Option[LocalDate]), ErrorResponse, CountriesStatisticsResponse, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("statistics" / "countries")
      .in(query[Option[String]]("period"))
      .in(query[Option[LocalDate]]("from"))
      .in(query[Option[LocalDate]]("to"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[CountriesStatisticsResponse])

  val getSpendingStatistics: Endpoint[String, (Option[LocalDate], Option[LocalDate], Option[String]), ErrorResponse, SpendingStatisticsResponse, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("statistics" / "spending")
      .in(query[Option[LocalDate]]("from"))
      .in(query[Option[LocalDate]]("to"))
      .in(query[Option[String]]("currency"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[SpendingStatisticsResponse])

  val getUpcomingTripsStatistics: Endpoint[String, Unit, ErrorResponse, UpcomingTripsResponse, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("statistics" / "upcoming-trips")
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[UpcomingTripsResponse])

  val getTripDurationsStatistics: Endpoint[String, (Option[LocalDate], Option[LocalDate]), ErrorResponse, TripDurationsResponse, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("statistics" / "trip-durations")
      .in(query[Option[LocalDate]]("from"))
      .in(query[Option[LocalDate]]("to"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[TripDurationsResponse])
}