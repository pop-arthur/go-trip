package gotrip.domain.additionalservice

import gotrip.domain.location.*
import gotrip.domain.provider.*

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
