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