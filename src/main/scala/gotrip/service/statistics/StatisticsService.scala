package gotrip.service.statistics

import cats.Monad
import cats.syntax.functor._
import gotrip.domain.statistics._
import gotrip.domain.user.UserId
import gotrip.repository.statistics.StatisticsRepository

final class StatisticsService[F[_]: Monad](
  repository: StatisticsRepository[F]
) {
  def getCountriesStatistics(userId: UserId, params: CountriesStatisticsParams): F[CountriesStatisticsResponse] =
    repository.getCountriesStatistics(userId, params)

  def getSpendingStatistics(userId: UserId, params: SpendingStatisticsParams): F[SpendingStatisticsResponse] =
    repository.getSpendingStatistics(userId, params)

  def getUpcomingTripsStatistics(userId: UserId): F[UpcomingTripsResponse] =
    repository.getUpcomingTripsStatistics(userId)

  def getTripDurationsStatistics(userId: UserId, params: TripDurationsParams): F[TripDurationsResponse] =
    repository.getTripDurationsStatistics(userId, params)
}