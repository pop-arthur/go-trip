package gotrip.http.additionalservice

import cats.data.ValidatedNel
import cats.syntax.apply.*
import cats.syntax.validated.*
import gotrip.domain.additionalservice.*
import gotrip.domain.validation.DomainValidation
import gotrip.domain.validation.DomainValidation.*

object AdditionalServiceValidator:

  def validate(service: AdditionalServiceCreate): ValidatedNel[DomainValidation, AdditionalServiceCreate] =
    (
      validateTitle(service.title),
      validateOptionalText(service.description, AdditionalServiceDescriptionIsBlank),
      validatePriceAmount(service.price_amount),
      validateOptionalText(service.price_currency, AdditionalServicePriceCurrencyIsBlank)
    ).mapN((_, _, _, _) => service)

  def validate(service: AdditionalServiceUpdate): ValidatedNel[DomainValidation, AdditionalServiceUpdate] =
    (
      service.title.fold(validUnit)(validateTitle),
      validateOptionalText(service.description, AdditionalServiceDescriptionIsBlank),
      validatePriceAmount(service.price_amount),
      validateOptionalText(service.price_currency, AdditionalServicePriceCurrencyIsBlank)
    ).mapN((_, _, _, _) => service)

  private def validateTitle(title: ServiceTitle): ValidatedNel[DomainValidation, Unit] =
    if title.value.trim.nonEmpty then validUnit
    else AdditionalServiceTitleIsBlank.invalidNel

  private def validateOptionalText(
    value: Option[String],
    error: DomainValidation
  ): ValidatedNel[DomainValidation, Unit] =
    value match
      case Some(text) if text.trim.isEmpty =>
        error.invalidNel
      case _ =>
        validUnit

  private def validatePriceAmount(value: Option[Double]): ValidatedNel[DomainValidation, Unit] =
    value match
      case Some(amount) if amount < 0 =>
        AdditionalServicePriceIsNegative.invalidNel
      case _ =>
        validUnit

  private def validUnit: ValidatedNel[DomainValidation, Unit] =
    ().validNel
