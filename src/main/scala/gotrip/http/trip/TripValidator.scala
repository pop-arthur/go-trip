package gotrip.http.trip

import cats.data.ValidatedNel
import gotrip.domain.trip.*
import gotrip.domain.validation.DomainValidation

object TripValidator:

  def validate(trip: TripCreate): ValidatedNel[DomainValidation, TripCreate] =
    TripCreate.validate(trip)

  def validate(trip: TripUpdate): ValidatedNel[DomainValidation, TripUpdate] =
    TripUpdate.validate(trip)
