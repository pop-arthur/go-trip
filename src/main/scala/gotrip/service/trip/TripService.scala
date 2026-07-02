package gotrip.service.trip

import cats.Monad
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.trip.*
import gotrip.domain.user.UserId
import gotrip.repository.trip.TripRepository
import gotrip.service.GeneratedData

final class TripService[F[_]: Sync: Clock](repository: TripRepository[F]):

  import TripServiceError.*

  def listByUser(userId: UserId, params: TripSearchParams): F[List[Trip]] =
    repository.listByUser(userId, params)

  def findByUser(userId: UserId, tripId: TripId): F[Either[TripServiceError, Trip]] =
    EitherT.fromOptionF(repository.findByUser(userId, tripId), TripNotFound(tripId)).value

  def create(userId: UserId, trip: TripCreate): F[Either[TripServiceError, Trip]] =
    (for {
      _ <- validateDateRange(trip.start_date.value, trip.end_date.value)
      materialized <- EitherT.liftF(materializeTrip(userId, trip))
      created <- EitherT.liftF(repository.create(materialized))
    } yield created).value

  def update(userId: UserId, tripId: TripId, trip: TripUpdate): F[Either[TripServiceError, Trip]] =
    (for {
      current <- EitherT.fromOptionF(repository.findByUser(userId, tripId), TripNotFound(tripId))
      _ <- validateDateRange(nextStartDate(current, trip), nextEndDate(current, trip))
      materialized <- EitherT.liftF(materializeTripUpdate(current, trip))
      updated <- EitherT.fromOptionF(repository.update(materialized), TripNotFound(tripId))
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
      (startDate, endDate) match
        case (Some(start), Some(end)) if start.isAfter(end) =>
          Left(InvalidDateRange)
        case _ =>
          Right(())
    }

  private def nextStartDate(current: Trip, update: TripUpdate): Option[java.time.LocalDate] =
    update.start_date match
      case Some(startDate) => startDate.value
      case None            => current.start_date.value

  private def nextEndDate(current: Trip, update: TripUpdate): Option[java.time.LocalDate] =
    update.end_date match
      case Some(endDate) => endDate.value
      case None          => current.end_date.value

  private def materializeTrip(userId: UserId, create: TripCreate): F[Trip] =
    for
      id <- GeneratedData.newId[F]
      now <- GeneratedData.now[F]
    yield Trip(
      id = TripId(id),
      user_id = userId,
      title = create.title,
      start_date = create.start_date,
      end_date = create.end_date,
      status = create.status.getOrElse(TripStatus.Planned),
      created_at = now,
      updated_at = now
    )

  private def materializeTripUpdate(current: Trip, update: TripUpdate): F[Trip] =
    GeneratedData.now[F].map { now =>
      current.copy(
        title = update.title.getOrElse(current.title),
        start_date = update.start_date.getOrElse(current.start_date),
        end_date = update.end_date.getOrElse(current.end_date),
        status = update.status.getOrElse(current.status),
        updated_at = now
      )
    }

enum TripServiceError:
  case TripNotFound(id: TripId)
  case InvalidDateRange
