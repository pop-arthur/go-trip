package gotrip.service.triplocation

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative.*
import cats.syntax.functor.*
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.repository.triplocation.TripLocationRepository

final class TripLocationService[F[_]: Monad](repository: TripLocationRepository[F]):

  import TripLocationServiceError.*

  def listByTrip(tripId: TripId): F[Either[TripLocationServiceError, List[TripLocation]]] =
    (for {
      _ <- ensureTripExists(tripId)
      locations <- EitherT.liftF(repository.listByTrip(tripId))
    } yield locations).value

  def create(
    tripId: TripId,
    location: TripLocationCreate
  ): F[Either[TripLocationServiceError, TripLocation]] =
    (for {
      _ <- validateDateRange(location.arrival_date.value, location.departure_date.value)
      _ <- ensureTripExists(tripId)
      _ <- ensureLocationExists(location.location_id)
      visitOrder <- EitherT.liftF(location.visit_order.fold(repository.nextVisitOrder(tripId))(_.pure[F]))
      _ <- ensureVisitOrderAvailable(tripId, visitOrder, None)
      created <- EitherT.liftF(repository.create(tripId, location, visitOrder))
    } yield created).value

  def update(
    tripId: TripId,
    tripLocationId: TripLocationId,
    location: TripLocationUpdate
  ): F[Either[TripLocationServiceError, TripLocation]] =
    (for {
      _ <- ensureTripExists(tripId)
      current <- EitherT.fromOptionF(
        repository.findInTrip(tripId, tripLocationId),
        TripLocationNotFound(tripLocationId)
      )
      _ <- validateDateRange(nextArrivalDate(current, location), nextDepartureDate(current, location))
      _ <- location.visit_order.fold(EitherT.rightT[F, TripLocationServiceError](())) { visitOrder =>
        ensureVisitOrderAvailable(tripId, visitOrder, Some(tripLocationId))
      }
      updated <- EitherT.fromOptionF(
        repository.update(tripId, tripLocationId, location),
        TripLocationNotFound(tripLocationId)
      )
    } yield updated).value

  def delete(
    tripId: TripId,
    tripLocationId: TripLocationId
  ): F[Either[TripLocationServiceError, Unit]] =
    (for {
      _ <- ensureTripExists(tripId)
      deleted <- EitherT.liftF(repository.delete(tripId, tripLocationId))
      _ <- if deleted then EitherT.rightT[F, TripLocationServiceError](())
           else EitherT.leftT[F, Unit](TripLocationNotFound(tripLocationId))
    } yield ()).value

  private def ensureTripExists(tripId: TripId): EitherT[F, TripLocationServiceError, Unit] =
    EitherT {
      repository.tripExists(tripId).map { exists =>
        Either.cond(exists, (), TripNotFound(tripId))
      }
    }

  private def ensureLocationExists(locationId: LocationId): EitherT[F, TripLocationServiceError, Unit] =
    EitherT {
      repository.locationExists(locationId).map { exists =>
        Either.cond(exists, (), LocationNotFound(locationId))
      }
    }

  private def ensureVisitOrderAvailable(
    tripId: TripId,
    visitOrder: VisitOrder,
    excludeTripLocationId: Option[TripLocationId]
  ): EitherT[F, TripLocationServiceError, Unit] =
    EitherT {
      repository.visitOrderExists(tripId, visitOrder, excludeTripLocationId).map { exists =>
        Either.cond(!exists, (), DuplicateVisitOrder(visitOrder))
      }
    }

  private def validateDateRange(
    arrivalDate: Option[java.time.OffsetDateTime],
    departureDate: Option[java.time.OffsetDateTime]
  ): EitherT[F, TripLocationServiceError, Unit] =
    EitherT.fromEither {
      (arrivalDate, departureDate) match
        case (Some(arrival), Some(departure)) if arrival.isAfter(departure) =>
          Left(InvalidDateRange)
        case _ =>
          Right(())
    }

  private def nextArrivalDate(
    current: TripLocation,
    update: TripLocationUpdate
  ): Option[java.time.OffsetDateTime] =
    update.arrival_date match
      case Some(arrivalDate) => arrivalDate.value
      case None              => current.arrival_date.value

  private def nextDepartureDate(
    current: TripLocation,
    update: TripLocationUpdate
  ): Option[java.time.OffsetDateTime] =
    update.departure_date match
      case Some(departureDate) => departureDate.value
      case None                => current.departure_date.value

enum TripLocationServiceError:
  case TripNotFound(id: TripId)
  case LocationNotFound(id: LocationId)
  case TripLocationNotFound(id: TripLocationId)
  case DuplicateVisitOrder(visitOrder: VisitOrder)
  case InvalidDateRange
