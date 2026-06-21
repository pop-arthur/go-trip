package gotrip.http.location

import cats.data.ValidatedNel
import cats.syntax.apply.*
import cats.syntax.validated.*
import gotrip.domain.location.*
import gotrip.domain.validation.DomainValidation
import gotrip.domain.validation.DomainValidation.*

object LocationValidator:

  def validate(location: LocationCreate): ValidatedNel[DomainValidation, LocationCreate] =
    (
      validateName(location.name),
      validateLatitude(location.latitude),
      validateLongitude(location.longitude)
    ).mapN((_, _, _) => location)

  def validate(location: LocationUpdate): ValidatedNel[DomainValidation, LocationUpdate] =
    (
      location.name.fold(validUnit)(validateName),
      location.latitude.fold(validUnit)(validateLatitude),
      location.longitude.fold(validUnit)(validateLongitude)
    ).mapN((_, _, _) => location)

  private def validateName(name: LocationName): ValidatedNel[DomainValidation, Unit] =
    if name.value.trim.nonEmpty then validUnit
    else LocationNameIsBlank.invalidNel

  private def validateLatitude(latitude: LocationLatitude): ValidatedNel[DomainValidation, Unit] =
    validateOptionalNumber(latitude.value, -90.0, 90.0, LatitudeOutOfRange)

  private def validateLongitude(longitude: LocationLongitude): ValidatedNel[DomainValidation, Unit] =
    validateOptionalNumber(longitude.value, -180.0, 180.0, LongitudeOutOfRange)

  private def validateOptionalNumber(
    value: Option[Double],
    min: Double,
    max: Double,
    error: DomainValidation
  ): ValidatedNel[DomainValidation, Unit] =
    value match
      case Some(number) if number < min || number > max =>
        error.invalidNel
      case _ =>
        validUnit

  private def validUnit: ValidatedNel[DomainValidation, Unit] =
    ().validNel
