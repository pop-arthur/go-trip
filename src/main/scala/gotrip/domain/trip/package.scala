package gotrip.domain

import java.time.OffsetDateTime
import scala.annotation.targetName

package object trip {
  opaque type TripId = Long
  object TripId {
    def apply(value: Long): TripId = value
  }
  extension (id: TripId) {
    def value: Long = id
  }

  opaque type TripLocationId = Long
  object TripLocationId {
    def apply(value: Long): TripLocationId = value
  }
  extension (id: TripLocationId) {
    @targetName("tripLocationIdValue")
    def value: Long = id
  }

  opaque type VisitOrder = Int
  object VisitOrder {
    def apply(value: Int): VisitOrder = value
  }
  extension (order: VisitOrder) {
    def value: Int = order
  }

  opaque type TripLocationArrivalDate = Option[OffsetDateTime]
  object TripLocationArrivalDate {
    def apply(value: Option[OffsetDateTime]): TripLocationArrivalDate = value
  }
  extension (arrivalDate: TripLocationArrivalDate) {
    @targetName("tripLocationArrivalDateValue")
    def value: Option[OffsetDateTime] = arrivalDate
  }

  opaque type TripLocationDepartureDate = Option[OffsetDateTime]
  object TripLocationDepartureDate {
    def apply(value: Option[OffsetDateTime]): TripLocationDepartureDate = value
  }
  extension (departureDate: TripLocationDepartureDate) {
    @targetName("tripLocationDepartureDateValue")
    def value: Option[OffsetDateTime] = departureDate
  }
}
