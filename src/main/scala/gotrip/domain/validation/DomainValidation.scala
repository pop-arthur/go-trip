package gotrip.domain.validation

import cats.data.ValidatedNel
import cats.syntax.functor.*
import cats.syntax.validated.*

sealed trait DomainValidation:
  def errorMessage: String

object DomainValidation:

  type Result[A] = ValidatedNel[DomainValidation, A]

  def valid[A](value: A): Result[A] =
    value.validNel

  def invalid[A](error: DomainValidation): Result[A] =
    error.invalidNel

  def validateNonBlank[A](
    value: String,
    error: DomainValidation
  )(construct: String => A): Result[A] =
    if isNonBlank(value) then valid(construct(value))
    else invalid(error)

  def validateOptional[A, B](
    value: Option[A]
  )(validate: A => Result[B]): Result[Option[B]] =
    value match
      case Some(actualValue) => validate(actualValue).map(Some(_))
      case None              => valid(None)

  def validateOptionalText(
    value: Option[String],
    error: DomainValidation
  ): Result[Option[String]] =
    validateOptional(value) { text =>
      if isNonBlank(text) then valid(text)
      else invalid(error)
    }

  def validatePositiveLong[A](
    value: Long,
    error: DomainValidation
  )(construct: Long => A): Result[A] =
    if value > 0 then valid(construct(value))
    else invalid(error)

  def validatePositiveInt[A](
    value: Int,
    error: DomainValidation
  )(construct: Int => A): Result[A] =
    if value > 0 then valid(construct(value))
    else invalid(error)

  def validateOptionalDoubleRange[A](
    value: Option[Double],
    min: Double,
    max: Double,
    error: DomainValidation
  )(construct: Option[Double] => A): Result[A] =
    value match
      case Some(actualValue) if actualValue < min || actualValue > max || actualValue.isNaN =>
        invalid(error)
      case _ =>
        valid(construct(value))

  def validateOptionalNonNegativeDouble(
    value: Option[Double],
    error: DomainValidation
  ): Result[Option[Double]] =
    value match
      case Some(actualValue) if actualValue < 0.0 || actualValue.isNaN =>
        invalid(error)
      case _ =>
        valid(value)

  private def isNonBlank(value: String): Boolean =
    value != null && value.trim.nonEmpty

  case object IdIsNotPositive extends DomainValidation:
    override val errorMessage: String = "Id must be greater than 0"

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

  case object TripTitleIsBlank extends DomainValidation:
    override val errorMessage: String = "Trip title must not be blank"

  case object InvalidTripDateRange extends DomainValidation:
    override val errorMessage: String = "Trip start date must be before or equal to end date"

  case object OrderTitleIsBlank extends DomainValidation:
    override val errorMessage: String = "Order title must not be blank"

  case object OrderExternalIdIsBlank extends DomainValidation:
    override val errorMessage: String = "External order id must not be blank"

  case object OrderPriceIsNegative extends DomainValidation:
    override val errorMessage: String = "Order price amount must be nonnegative"

  case object OrderPriceCurrencyIsBlank extends DomainValidation:
    override val errorMessage: String = "Order price currency must not be blank"

  case object InvalidOrderDateTimeRange extends DomainValidation:
    override val errorMessage: String = "Order start datetime must be before or equal to end datetime"

  case object OrderStatusReasonIsBlank extends DomainValidation:
    override val errorMessage: String = "Order status reason must not be blank"

  case object OrderFileUrlIsBlank extends DomainValidation:
    override val errorMessage: String = "Order file URL must not be blank"

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
