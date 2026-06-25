package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import java.time.OffsetDateTime
import scala.annotation.targetName

package object trip {
  opaque type TripId = Long
  object TripId {
    def apply(value: Long): TripId = value

    def from(value: Long): Result[TripId] =
      validatePositiveLong(value, IdIsNotPositive)(TripId.apply)
  }
  extension (id: TripId) {
    def value: Long = id
  }

  opaque type TripLocationId = Long
  object TripLocationId {
    def apply(value: Long): TripLocationId = value

    def from(value: Long): Result[TripLocationId] =
      validatePositiveLong(value, IdIsNotPositive)(TripLocationId.apply)
  }
  extension (id: TripLocationId) {
    @targetName("tripLocationIdValue")
    def value: Long = id
  }

  opaque type VisitOrder = Int
  object VisitOrder {
    def apply(value: Int): VisitOrder = value

    def from(value: Int): Result[VisitOrder] =
      validatePositiveInt(value, VisitOrderIsNotPositive)(VisitOrder.apply)
  }
  extension (order: VisitOrder) {
    def value: Int = order
  }

  opaque type TripLocationArrivalDate = Option[OffsetDateTime]
  object TripLocationArrivalDate {
    def apply(value: Option[OffsetDateTime]): TripLocationArrivalDate = value

    def from(value: Option[OffsetDateTime]): Result[TripLocationArrivalDate] =
      valid(TripLocationArrivalDate(value))
  }
  extension (arrivalDate: TripLocationArrivalDate) {
    @targetName("tripLocationArrivalDateValue")
    def value: Option[OffsetDateTime] = arrivalDate
  }

  opaque type TripLocationDepartureDate = Option[OffsetDateTime]
  object TripLocationDepartureDate {
    def apply(value: Option[OffsetDateTime]): TripLocationDepartureDate = value

    def from(value: Option[OffsetDateTime]): Result[TripLocationDepartureDate] =
      valid(TripLocationDepartureDate(value))
  }
  extension (departureDate: TripLocationDepartureDate) {
    @targetName("tripLocationDepartureDateValue")
    def value: Option[OffsetDateTime] = departureDate
  }
}
