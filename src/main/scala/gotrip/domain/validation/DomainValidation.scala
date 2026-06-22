package gotrip.domain.validation

sealed trait DomainValidation:
  def errorMessage: String

object DomainValidation:

  case object LocationNameIsBlank extends DomainValidation:
    override val errorMessage: String = "Location name must not be blank"

  case object LatitudeOutOfRange extends DomainValidation:
    override val errorMessage: String = "Latitude must be between -90.0 and 90.0"

  case object LongitudeOutOfRange extends DomainValidation:
    override val errorMessage: String = "Longitude must be between -180.0 and 180.0"

  case object VisitOrderIsNotPositive extends DomainValidation:
    override val errorMessage: String = "Visit order must be greater than 0"

  case object InvalidTripLocationDateRange extends DomainValidation:
    override val errorMessage: String = "Arrival date must be before or equal to departure date"

  case object ProviderNameIsBlank extends DomainValidation:
    override val errorMessage: String = "Provider name must not be blank"

  case object ProviderWebsiteIsBlank extends DomainValidation:
    override val errorMessage: String = "Website must not be blank"

  case object ProviderSupportContactIsBlank extends DomainValidation:
    override val errorMessage: String = "Support contact must not be blank"

  case object AdditionalServiceTitleIsBlank extends DomainValidation:
    override val errorMessage: String = "Additional service title must not be blank"

  case object AdditionalServiceDescriptionIsBlank extends DomainValidation:
    override val errorMessage: String = "Description must not be blank"

  case object AdditionalServicePriceIsNegative extends DomainValidation:
    override val errorMessage: String = "Price amount must be nonnegative"

  case object AdditionalServicePriceCurrencyIsBlank extends DomainValidation:
    override val errorMessage: String = "Price currency must not be blank"
