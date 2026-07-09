package gotrip.service.trip

import cats.Monad
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.functor._
import gotrip.domain.trip._
import gotrip.domain.user.UserId
import gotrip.repository.trip.TripRepository
import gotrip.service.GeneratedData
import gotrip.service.achievement.{AchievementEngine, AchievementEvent}

final class TripService[F[_]: Sync: Clock: GeneratedData](
  repository: TripRepository[F],
  achievementEngine: AchievementEngine[F]
) {

  import TripServiceError._

  def listByUser(userId: UserId, params: TripSearchParams): F[List[Trip]] =
    repository.listByUser(userId, params)

  def findByUser(userId: UserId, tripId: TripId): F[Either[TripServiceError, Trip]] =
    EitherT.fromOptionF(repository.findByUser(userId, tripId), TripNotFound(tripId)).value

  def create(userId: UserId, trip: TripCreate): F[Either[TripServiceError, Trip]] =
    (for {
      _ <- validateDateRange(trip.start_date.value, trip.end_date.value)
      created <- EitherT.liftF(repository.create(userId, trip))
      _ <- EitherT.liftF(achievementEngine.checkAndUnlock(userId, AchievementEvent.TripCreated(created)))
    } yield created).value

  def update(userId: UserId, tripId: TripId, trip: TripUpdate): F[Either[TripServiceError, Trip]] =
    (for {
      current <- EitherT.fromOptionF(repository.findByUser(userId, tripId), TripNotFound(tripId))
      _ <- validateDateRange(nextStartDate(current, trip), nextEndDate(current, trip))
      updated <- EitherT.fromOptionF(repository.update(userId, tripId, trip), TripNotFound(tripId))
      _ <- if (updated.status == TripStatus.Completed && current.status != TripStatus.Completed)
             EitherT.liftF(achievementEngine.checkAndUnlock(userId, AchievementEvent.TripCompleted(updated)))
           else EitherT.rightT(())
    } yield updated).value

  def delete(userId: UserId, tripId: TripId): F[Either[TripServiceError, Unit]] =
    (for {
      deleted <- EitherT.liftF(repository.delete(userId, tripId))
      _ <- if deleted then EitherT.rightT[F, TripServiceError](())
           else EitherT.leftT[F, Unit](TripNotFound(tripId))
    } yield ()).value

  private def validateDateRange(
    startDate: Option[java.time.LocalDate],
    endDate: Option[java.time.LocalDate]
  ): EitherT[F, TripServiceError, Unit] =
    EitherT.fromEither {
      (startDate, endDate) match {
        case (Some(start), Some(end)) if start.isAfter(end) =>
          Left(InvalidDateRange)
        case _ =>
          Right(())
      }
    }

  private def nextStartDate(current: Trip, update: TripUpdate): Option[java.time.LocalDate] =
    update.start_date match {
      case Some(startDate) => startDate.value
      case None            => current.start_date.value
    }

  private def nextEndDate(current: Trip, update: TripUpdate): Option[java.time.LocalDate] =
    update.end_date match {
      case Some(endDate) => endDate.value
      case None          => current.end_date.value
    }
}

enum TripServiceError {
  case TripNotFound(id: TripId)
  case InvalidDateRange
}