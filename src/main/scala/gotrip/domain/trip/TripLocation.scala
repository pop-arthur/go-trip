package gotrip.domain.trip

import cats.syntax.apply.*
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import java.time.OffsetDateTime
import java.util.UUID

final case class TripLocation(
  id: TripLocationId,
  trip_id: TripId,
  location_id: LocationId,
  visit_order: VisitOrder,
  arrival_date: TripLocationArrivalDate,
  departure_date: TripLocationDepartureDate
)

object TripLocation:

  def validateDateRange(
    arrivalDate: Option[OffsetDateTime],
    departureDate: Option[OffsetDateTime]
  ): Result[Unit] =
    (arrivalDate, departureDate) match
      case (Some(arrival), Some(departure)) if arrival.isAfter(departure) =>
        invalid(InvalidTripLocationDateRange)
      case _ =>
        valid(())

final case class TripLocationCreate(
  location_id: LocationId,
  visit_order: Option[VisitOrder] = None,
  arrival_date: TripLocationArrivalDate = TripLocationArrivalDate(None),
  departure_date: TripLocationDepartureDate = TripLocationDepartureDate(None)
)

object TripLocationCreate:

  def from(
    locationId: UUID,
    visitOrder: Option[Int] = None,
    arrivalDate: Option[OffsetDateTime] = None,
    departureDate: Option[OffsetDateTime] = None
  ): Result[TripLocationCreate] =
    (
      LocationId.from(locationId),
      validateOptional(visitOrder)(VisitOrder.from),
      TripLocationArrivalDate.from(arrivalDate),
      TripLocationDepartureDate.from(departureDate),
      TripLocation.validateDateRange(arrivalDate, departureDate)
    ).mapN { (validLocationId, validVisitOrder, validArrivalDate, validDepartureDate, _) =>
      TripLocationCreate(
        location_id = validLocationId,
        visit_order = validVisitOrder,
        arrival_date = validArrivalDate,
        departure_date = validDepartureDate
      )
    }

  def validate(location: TripLocationCreate): Result[TripLocationCreate] =
    from(
      locationId = location.location_id.value,
      visitOrder = location.visit_order.map(_.value),
      arrivalDate = location.arrival_date.value,
      departureDate = location.departure_date.value
    )

final case class TripLocationUpdate(
  visit_order: Option[VisitOrder] = None,
  arrival_date: Option[TripLocationArrivalDate] = None,
  departure_date: Option[TripLocationDepartureDate] = None
)

object TripLocationUpdate:

  def validate(location: TripLocationUpdate): Result[TripLocationUpdate] =
    (
      validateOptional(location.visit_order)(visitOrder => VisitOrder.from(visitOrder.value)),
      validateOptional(location.arrival_date)(arrivalDate => TripLocationArrivalDate.from(arrivalDate.value)),
      validateOptional(location.departure_date)(departureDate => TripLocationDepartureDate.from(departureDate.value)),
      TripLocation.validateDateRange(
        location.arrival_date.flatMap(_.value),
        location.departure_date.flatMap(_.value)
      )
    ).mapN { (validVisitOrder, validArrivalDate, validDepartureDate, _) =>
      location.copy(
        visit_order = validVisitOrder,
        arrival_date = validArrivalDate,
        departure_date = validDepartureDate
      )
    }
