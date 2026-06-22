package gotrip.http.additionalservice

import cats.data.ValidatedNel
import gotrip.domain.additionalservice.*
import gotrip.domain.validation.DomainValidation

object AdditionalServiceValidator:

  def validate(service: AdditionalServiceCreate): ValidatedNel[DomainValidation, AdditionalServiceCreate] =
    AdditionalServiceCreate.validate(service)

  def validate(service: AdditionalServiceUpdate): ValidatedNel[DomainValidation, AdditionalServiceUpdate] =
    AdditionalServiceUpdate.validate(service)
