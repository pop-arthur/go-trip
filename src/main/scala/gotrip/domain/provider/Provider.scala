package gotrip.domain.provider

import cats.syntax.apply.*
import gotrip.domain.provider.*
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

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

object ProviderCreate:

  def from(
    name: String,
    providerType: ProviderType,
    website: Option[String] = None,
    supportContact: Option[String] = None
  ): Result[ProviderCreate] =
    (
      ProviderName.from(name),
      validateOptionalText(website, ProviderWebsiteIsBlank),
      validateOptionalText(supportContact, ProviderSupportContactIsBlank)
    ).mapN { (validName, validWebsite, validSupportContact) =>
      ProviderCreate(
        name = validName,
        `type` = providerType,
        website = validWebsite,
        support_contact = validSupportContact
      )
    }

  def validate(provider: ProviderCreate): Result[ProviderCreate] =
    from(
      name = provider.name.value,
      providerType = provider.`type`,
      website = provider.website,
      supportContact = provider.support_contact
    )

final case class ProviderUpdate(
  name: Option[ProviderName] = None,
  `type`: Option[ProviderType] = None,
  website: Option[String] = None,
  support_contact: Option[String] = None
)

object ProviderUpdate:

  def from(
    name: Option[String] = None,
    providerType: Option[ProviderType] = None,
    website: Option[String] = None,
    supportContact: Option[String] = None
  ): Result[ProviderUpdate] =
    (
      validateOptional(name)(ProviderName.from),
      validateOptionalText(website, ProviderWebsiteIsBlank),
      validateOptionalText(supportContact, ProviderSupportContactIsBlank)
    ).mapN { (validName, validWebsite, validSupportContact) =>
      ProviderUpdate(
        name = validName,
        `type` = providerType,
        website = validWebsite,
        support_contact = validSupportContact
      )
    }

  def validate(provider: ProviderUpdate): Result[ProviderUpdate] =
    (
      validateOptional(provider.name)(name => ProviderName.from(name.value)),
      validateOptionalText(provider.website, ProviderWebsiteIsBlank),
      validateOptionalText(provider.support_contact, ProviderSupportContactIsBlank)
    ).mapN { (validName, validWebsite, validSupportContact) =>
      provider.copy(
        name = validName,
        website = validWebsite,
        support_contact = validSupportContact
      )
    }
