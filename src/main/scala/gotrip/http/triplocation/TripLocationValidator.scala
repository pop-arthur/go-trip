package gotrip.http.triplocation

import cats.data.ValidatedNel
import gotrip.domain.trip.*
import gotrip.domain.validation.DomainValidation

object TripLocationValidator:

  def validate(location: TripLocationCreate): ValidatedNel[DomainValidation, TripLocationCreate] =
    TripLocationCreate.validate(location)

  def validate(location: TripLocationUpdate): ValidatedNel[DomainValidation, TripLocationUpdate] =
    TripLocationUpdate.validate(location)
