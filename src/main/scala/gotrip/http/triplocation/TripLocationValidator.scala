package gotrip.http.triplocation

import cats.data.ValidatedNel
import cats.syntax.validated.*
import gotrip.domain.trip.*
import gotrip.domain.validation.DomainValidation
import gotrip.domain.validation.DomainValidation.*

object TripLocationValidator:

  def validate(location: TripLocationCreate): ValidatedNel[DomainValidation, TripLocationCreate] =
    validateDateRange(location.arrival_date, location.departure_date).map(_ => location)

  def validate(location: TripLocationUpdate): ValidatedNel[DomainValidation, TripLocationUpdate] =
    validateProvidedDateRange(location.arrival_date, location.departure_date).map(_ => location)

  private def validateDateRange(
    arrivalDate: TripLocationArrivalDate,
    departureDate: TripLocationDepartureDate
  ): ValidatedNel[DomainValidation, Unit] =
    validateOptionalDateRange(arrivalDate.value, departureDate.value)

  private def validateProvidedDateRange(
    arrivalDate: Option[TripLocationArrivalDate],
    departureDate: Option[TripLocationDepartureDate]
  ): ValidatedNel[DomainValidation, Unit] =
    validateOptionalDateRange(arrivalDate.flatMap(_.value), departureDate.flatMap(_.value))

  private def validateOptionalDateRange(
    arrivalDate: Option[java.time.OffsetDateTime],
    departureDate: Option[java.time.OffsetDateTime]
  ): ValidatedNel[DomainValidation, Unit] =
    (arrivalDate, departureDate) match
      case (Some(arrival), Some(departure)) if arrival.isAfter(departure) =>
        InvalidTripLocationDateRange.invalidNel
      case _ =>
        validUnit

  private def validUnit: ValidatedNel[DomainValidation, Unit] =
    ().validNel
