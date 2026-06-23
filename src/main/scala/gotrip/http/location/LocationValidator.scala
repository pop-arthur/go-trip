package gotrip.http.location

import cats.data.ValidatedNel
import cats.syntax.validated.*
import gotrip.domain.location.*
import gotrip.domain.validation.DomainValidation
import gotrip.domain.validation.DomainValidation.*

object LocationValidator:

  def validate(location: LocationCreate): ValidatedNel[DomainValidation, LocationCreate] =
    validateName(location.name).map(_ => location)

  def validate(location: LocationUpdate): ValidatedNel[DomainValidation, LocationUpdate] =
    location.name.fold(validUnit)(validateName).map(_ => location)

  private def validateName(name: LocationName): ValidatedNel[DomainValidation, Unit] =
    if name.value.trim.nonEmpty then validUnit
    else LocationNameIsBlank.invalidNel

  private def validUnit: ValidatedNel[DomainValidation, Unit] =
    ().validNel
