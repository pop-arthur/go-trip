package gotrip.http.provider

import cats.data.ValidatedNel
import cats.syntax.apply.*
import cats.syntax.validated.*
import gotrip.domain.provider.*
import gotrip.domain.validation.DomainValidation
import gotrip.domain.validation.DomainValidation.*

object ProviderValidator:

  def validate(provider: ProviderCreate): ValidatedNel[DomainValidation, ProviderCreate] =
    (
      validateName(provider.name),
      validateOptionalText(provider.website, ProviderWebsiteIsBlank),
      validateOptionalText(provider.support_contact, ProviderSupportContactIsBlank)
    ).mapN((_, _, _) => provider)

  def validate(provider: ProviderUpdate): ValidatedNel[DomainValidation, ProviderUpdate] =
    (
      provider.name.fold(validUnit)(validateName),
      validateOptionalText(provider.website, ProviderWebsiteIsBlank),
      validateOptionalText(provider.support_contact, ProviderSupportContactIsBlank)
    ).mapN((_, _, _) => provider)

  private def validateName(name: ProviderName): ValidatedNel[DomainValidation, Unit] =
    if name.value.trim.nonEmpty then validUnit
    else ProviderNameIsBlank.invalidNel

  private def validateOptionalText(
    value: Option[String],
    error: DomainValidation
  ): ValidatedNel[DomainValidation, Unit] =
    value match
      case Some(text) if text.trim.isEmpty =>
        error.invalidNel
      case _ =>
        validUnit

  private def validUnit: ValidatedNel[DomainValidation, Unit] =
    ().validNel
