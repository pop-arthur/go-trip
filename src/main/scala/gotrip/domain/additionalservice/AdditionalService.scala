package gotrip.domain.additionalservice

import cats.syntax.apply.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

enum ServiceType:
  case Flight, Train, Bus, Hotel, Tour, CarRental, Insurance, Taxi, Esim, Lounge, ExtraBaggage, Other

final case class AdditionalService(
  id: ServiceId,
  title: ServiceTitle,
  description: Option[String],
  service_type: ServiceType,
  provider_id: Option[ProviderId],
  location_id: Option[LocationId],
  price_amount: Option[Double],
  price_currency: Option[String],
  is_active: Boolean
)

final case class AdditionalServiceSearchParams(
  serviceType: Option[ServiceType] = None,
  locationId: Option[LocationId] = None,
  providerId: Option[ProviderId] = None
)

final case class AdditionalServiceCreate(
  title: ServiceTitle,
  description: Option[String] = None,
  service_type: ServiceType,
  provider_id: Option[ProviderId] = None,
  location_id: Option[LocationId] = None,
  price_amount: Option[Double] = None,
  price_currency: Option[String] = None,
  is_active: Option[Boolean] = None
)

object AdditionalServiceCreate:

  def from(
    title: String,
    serviceType: ServiceType,
    description: Option[String] = None,
    providerId: Option[Long] = None,
    locationId: Option[Long] = None,
    priceAmount: Option[Double] = None,
    priceCurrency: Option[String] = None,
    isActive: Option[Boolean] = None
  ): Result[AdditionalServiceCreate] =
    (
      ServiceTitle.from(title),
      validateOptionalText(description, AdditionalServiceDescriptionIsBlank),
      validateOptional(providerId)(ProviderId.from),
      validateOptional(locationId)(LocationId.from),
      validateOptionalNonNegativeDouble(priceAmount, AdditionalServicePriceIsNegative),
      validateOptionalText(priceCurrency, AdditionalServicePriceCurrencyIsBlank)
    ).mapN { (validTitle, validDescription, validProviderId, validLocationId, validPriceAmount, validPriceCurrency) =>
      AdditionalServiceCreate(
        title = validTitle,
        description = validDescription,
        service_type = serviceType,
        provider_id = validProviderId,
        location_id = validLocationId,
        price_amount = validPriceAmount,
        price_currency = validPriceCurrency,
        is_active = isActive
      )
    }

  def validate(service: AdditionalServiceCreate): Result[AdditionalServiceCreate] =
    from(
      title = service.title.value,
      description = service.description,
      serviceType = service.service_type,
      providerId = service.provider_id.map(_.value),
      locationId = service.location_id.map(_.value),
      priceAmount = service.price_amount,
      priceCurrency = service.price_currency,
      isActive = service.is_active
    )

final case class AdditionalServiceUpdate(
  title: Option[ServiceTitle] = None,
  description: Option[String] = None,
  service_type: Option[ServiceType] = None,
  provider_id: Option[ProviderId] = None,
  location_id: Option[LocationId] = None,
  price_amount: Option[Double] = None,
  price_currency: Option[String] = None,
  is_active: Option[Boolean] = None
)

object AdditionalServiceUpdate:

  def validate(service: AdditionalServiceUpdate): Result[AdditionalServiceUpdate] =
    (
      validateOptional(service.title)(title => ServiceTitle.from(title.value)),
      validateOptionalText(service.description, AdditionalServiceDescriptionIsBlank),
      validateOptional(service.provider_id)(providerId => ProviderId.from(providerId.value)),
      validateOptional(service.location_id)(locationId => LocationId.from(locationId.value)),
      validateOptionalNonNegativeDouble(service.price_amount, AdditionalServicePriceIsNegative),
      validateOptionalText(service.price_currency, AdditionalServicePriceCurrencyIsBlank)
    ).mapN { (validTitle, validDescription, validProviderId, validLocationId, validPriceAmount, validPriceCurrency) =>
      service.copy(
        title = validTitle,
        description = validDescription,
        provider_id = validProviderId,
        location_id = validLocationId,
        price_amount = validPriceAmount,
        price_currency = validPriceCurrency
      )
    }
