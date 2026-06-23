package gotrip.domain.trip

import gotrip.domain.location.*

final case class TripLocation(
  id: TripLocationId,
  trip_id: TripId,
  location_id: LocationId,
  visit_order: VisitOrder,
  arrival_date: TripLocationArrivalDate,
  departure_date: TripLocationDepartureDate
)

final case class TripLocationCreate(
  location_id: LocationId,
  visit_order: Option[VisitOrder] = None,
  arrival_date: TripLocationArrivalDate = TripLocationArrivalDate(None),
  departure_date: TripLocationDepartureDate = TripLocationDepartureDate(None)
)

final case class TripLocationUpdate(
  visit_order: Option[VisitOrder] = None,
  arrival_date: Option[TripLocationArrivalDate] = None,
  departure_date: Option[TripLocationDepartureDate] = None
)
