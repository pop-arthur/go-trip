package gotrip.http.statistics

import cats.effect.IO
import gotrip.domain.statistics._
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.statistics.StatisticsService
import sttp.tapir.server.ServerEndpoint

final class StatisticsController(
  service: StatisticsService[IO],
  authSupport: AuthSupport
) {

  val getCountriesStatistics: ServerEndpoint[Any, IO] =
    StatisticsEndpoints.getCountriesStatistics
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (period, from, to) =>
        val params = CountriesStatisticsParams(period, from, to)
        service.getCountriesStatistics(user.userId, params).attempt.map {
          case Right(response) => Right(response)
          case Left(error)     => Left(HttpError.Internal(error.getMessage))
        }
      }}

  val getSpendingStatistics: ServerEndpoint[Any, IO] =
    StatisticsEndpoints.getSpendingStatistics
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (from, to, currency) =>
        val params = SpendingStatisticsParams(from, to, currency)
        service.getSpendingStatistics(user.userId, params).attempt.map {
          case Right(response) => Right(response)
          case Left(error)     => Left(HttpError.Internal(error.getMessage))
        }
      }}

  val getUpcomingTripsStatistics: ServerEndpoint[Any, IO] =
    StatisticsEndpoints.getUpcomingTripsStatistics
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => _ =>
        service.getUpcomingTripsStatistics(user.userId).attempt.map {
          case Right(response) => Right(response)
          case Left(error)     => Left(HttpError.Internal(error.getMessage))
        }
      }

  val getTripDurationsStatistics: ServerEndpoint[Any, IO] =
    StatisticsEndpoints.getTripDurationsStatistics
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (from, to) =>
        val params = TripDurationsParams(from, to)
        service.getTripDurationsStatistics(user.userId, params).attempt.map {
          case Right(response) => Right(response)
          case Left(error)     => Left(HttpError.Internal(error.getMessage))
        }
      }}

  val all: List[ServerEndpoint[Any, IO]] =
    List(
      getCountriesStatistics,
      getSpendingStatistics,
      getUpcomingTripsStatistics,
      getTripDurationsStatistics
    )
}