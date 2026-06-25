package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import java.time.OffsetDateTime
import java.time.LocalDate
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

  opaque type TripTitle = String
  object TripTitle {
    def apply(value: String): TripTitle = value

    def from(value: String): Result[TripTitle] =
      validateNonBlank(value, TripTitleIsBlank)(TripTitle.apply)
  }
  extension (title: TripTitle) {
    @targetName("tripTitleValue")
    def value: String = title
  }

  opaque type TripStartDate = Option[LocalDate]
  object TripStartDate {
    def apply(value: Option[LocalDate]): TripStartDate = value

    def from(value: Option[LocalDate]): Result[TripStartDate] =
      valid(TripStartDate(value))
  }
  extension (startDate: TripStartDate) {
    @targetName("tripStartDateValue")
    def value: Option[LocalDate] = startDate
  }

  opaque type TripEndDate = Option[LocalDate]
  object TripEndDate {
    def apply(value: Option[LocalDate]): TripEndDate = value

    def from(value: Option[LocalDate]): Result[TripEndDate] =
      valid(TripEndDate(value))
  }
  extension (endDate: TripEndDate) {
    @targetName("tripEndDateValue")
    def value: Option[LocalDate] = endDate
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
