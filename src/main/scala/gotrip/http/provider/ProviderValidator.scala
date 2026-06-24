package gotrip.http.provider

import cats.data.ValidatedNel
import gotrip.domain.provider.*
import gotrip.domain.validation.DomainValidation

object ProviderValidator:

  def validate(provider: ProviderCreate): ValidatedNel[DomainValidation, ProviderCreate] =
    ProviderCreate.validate(provider)

  def validate(provider: ProviderUpdate): ValidatedNel[DomainValidation, ProviderUpdate] =
    ProviderUpdate.validate(provider)
