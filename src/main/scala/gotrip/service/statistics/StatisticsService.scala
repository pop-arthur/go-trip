package gotrip.service.statistics

import cats.Monad
import cats.syntax.functor._
import gotrip.domain.statistics._
import gotrip.domain.user.UserId
import gotrip.repository.statistics.StatisticsRepository

trait StatisticsService[F[_]] {
  def getCountriesStatistics(userId: UserId, params: CountriesStatisticsParams): F[CountriesStatisticsResponse]
  def getSpendingStatistics(userId: UserId, params: SpendingStatisticsParams): F[SpendingStatisticsResponse]
  def getUpcomingTripsStatistics(userId: UserId): F[UpcomingTripsResponse]
  def getTripDurationsStatistics(userId: UserId, params: TripDurationsParams): F[TripDurationsResponse]
}

object StatisticsService {
  def make[F[_]: Monad](repo: StatisticsRepository[F]): StatisticsService[F] =
    new StatisticsService[F] {
      override def getCountriesStatistics(userId: UserId, params: CountriesStatisticsParams): F[CountriesStatisticsResponse] =
        repo.getCountriesStatistics(userId, params)

      override def getSpendingStatistics(userId: UserId, params: SpendingStatisticsParams): F[SpendingStatisticsResponse] =
        repo.getSpendingStatistics(userId, params)

      override def getUpcomingTripsStatistics(userId: UserId): F[UpcomingTripsResponse] =
        repo.getUpcomingTripsStatistics(userId)

      override def getTripDurationsStatistics(userId: UserId, params: TripDurationsParams): F[TripDurationsResponse] =
        repo.getTripDurationsStatistics(userId, params)
    }
}