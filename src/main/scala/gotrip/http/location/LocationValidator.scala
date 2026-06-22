package gotrip.http.location

import cats.data.ValidatedNel
import gotrip.domain.location.*
import gotrip.domain.validation.DomainValidation

object LocationValidator:

  def validate(location: LocationCreate): ValidatedNel[DomainValidation, LocationCreate] =
    LocationCreate.validate(location)

  def validate(location: LocationUpdate): ValidatedNel[DomainValidation, LocationUpdate] =
    LocationUpdate.validate(location)
