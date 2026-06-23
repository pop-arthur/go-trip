package gotrip.domain.provider

enum ProviderType:
  case Airline, Hotel, TourCompany, TransportCompany, BookingPlatform, InsuranceCompany, Other

final case class Provider(
  id: ProviderId,
  name: ProviderName,
  `type`: ProviderType,
  website: Option[String],
  support_contact: Option[String]
)

final case class ProviderSearchParams(
  `type`: Option[ProviderType] = None,
  query: Option[String] = None
)

final case class ProviderCreate(
  name: ProviderName,
  `type`: ProviderType,
  website: Option[String] = None,
  support_contact: Option[String] = None
)

final case class ProviderUpdate(
  name: Option[ProviderName] = None,
  `type`: Option[ProviderType] = None,
  website: Option[String] = None,
  support_contact: Option[String] = None
)
