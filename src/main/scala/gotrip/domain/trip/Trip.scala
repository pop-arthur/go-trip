package gotrip.domain.trip

import cats.syntax.apply.*
import gotrip.domain.user.UserId
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import java.time.{Instant, LocalDate}

enum TripStatus:
  case Planned, Active, Completed, Cancelled

final case class Trip(
  id: TripId,
  user_id: UserId,
  title: TripTitle,
  start_date: TripStartDate,
  end_date: TripEndDate,
  status: TripStatus,
  created_at: Instant,
  updated_at: Instant
)

object Trip:

  def validateDateRange(
    startDate: Option[LocalDate],
    endDate: Option[LocalDate]
  ): Result[Unit] =
    (startDate, endDate) match
      case (Some(start), Some(end)) if start.isAfter(end) =>
        invalid(InvalidTripDateRange)
      case _ =>
        valid(())

final case class TripCreate(
  title: TripTitle,
  start_date: TripStartDate = TripStartDate(None),
  end_date: TripEndDate = TripEndDate(None),
  status: Option[TripStatus] = None
)

object TripCreate:

  def from(
    title: String,
    startDate: Option[LocalDate] = None,
    endDate: Option[LocalDate] = None,
    status: Option[TripStatus] = None
  ): Result[TripCreate] =
    (
      TripTitle.from(title),
      TripStartDate.from(startDate),
      TripEndDate.from(endDate),
      Trip.validateDateRange(startDate, endDate)
    ).mapN { (validTitle, validStartDate, validEndDate, _) =>
      TripCreate(
        title = validTitle,
        start_date = validStartDate,
        end_date = validEndDate,
        status = status
      )
    }

  def validate(trip: TripCreate): Result[TripCreate] =
    from(
      title = trip.title.value,
      startDate = trip.start_date.value,
      endDate = trip.end_date.value,
      status = trip.status
    )

final case class TripUpdate(
  title: Option[TripTitle] = None,
  start_date: Option[TripStartDate] = None,
  end_date: Option[TripEndDate] = None,
  status: Option[TripStatus] = None
)

object TripUpdate:

  def validate(trip: TripUpdate): Result[TripUpdate] =
    (
      validateOptional(trip.title)(title => TripTitle.from(title.value)),
      validateOptional(trip.start_date)(startDate => TripStartDate.from(startDate.value)),
      validateOptional(trip.end_date)(endDate => TripEndDate.from(endDate.value)),
      Trip.validateDateRange(
        trip.start_date.flatMap(_.value),
        trip.end_date.flatMap(_.value)
      )
    ).mapN { (validTitle, validStartDate, validEndDate, _) =>
      trip.copy(
        title = validTitle,
        start_date = validStartDate,
        end_date = validEndDate
      )
    }

final case class TripSearchParams(
  status: Option[TripStatus] = None,
  fromDate: Option[LocalDate] = None,
  toDate: Option[LocalDate] = None
)
