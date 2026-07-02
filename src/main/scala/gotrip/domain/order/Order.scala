package gotrip.domain.order

import cats.syntax.apply.*
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.user.UserId
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*
import io.circe.Json

import java.time.{Instant, LocalDate, OffsetDateTime}
import java.util.UUID

enum OrderStatus:
  case PendingVerification, Confirmed, Delayed, Cancelled, Completed, RefundPending, Refunded

enum FileType:
  case Pdf, Image, Email, Json, Other

enum OrderStatusEventSource:
  case System, AdminSimulation, UserEdit

final case class Order(
  id: OrderId,
  user_id: UserId,
  trip_id: TripId,
  provider_id: Option[ProviderId],
  service_type: ServiceType,
  external_order_id: Option[String],
  title: OrderTitle,
  status: OrderStatus,
  price_amount: Option[Double],
  price_currency: Option[String],
  start_datetime: Option[OffsetDateTime],
  end_datetime: Option[OffsetDateTime],
  departure_location_id: Option[LocationId],
  arrival_location_id: Option[LocationId],
  created_at: Instant,
  updated_at: Instant
)

object Order:

  def validateDateTimeRange(
    startDateTime: Option[OffsetDateTime],
    endDateTime: Option[OffsetDateTime]
  ): Result[Unit] =
    (startDateTime, endDateTime) match
      case (Some(start), Some(end)) if start.isAfter(end) =>
        invalid(InvalidOrderDateTimeRange)
      case _ =>
        valid(())

final case class OrderCreate(
  provider_id: Option[ProviderId] = None,
  service_type: ServiceType,
  external_order_id: Option[String] = None,
  title: OrderTitle,
  status: Option[OrderStatus] = None,
  price_amount: Option[Double] = None,
  price_currency: Option[String] = None,
  start_datetime: Option[OffsetDateTime] = None,
  end_datetime: Option[OffsetDateTime] = None,
  departure_location_id: Option[LocationId] = None,
  arrival_location_id: Option[LocationId] = None
)

object OrderCreate:

  def from(
    providerId: Option[UUID] = None,
    serviceType: ServiceType,
    externalOrderId: Option[String] = None,
    title: String,
    status: Option[OrderStatus] = None,
    priceAmount: Option[Double] = None,
    priceCurrency: Option[String] = None,
    startDateTime: Option[OffsetDateTime] = None,
    endDateTime: Option[OffsetDateTime] = None,
    departureLocationId: Option[UUID] = None,
    arrivalLocationId: Option[UUID] = None
  ): Result[OrderCreate] =
    (
      validateOptional(providerId)(ProviderId.from),
      validateOptionalText(externalOrderId, OrderExternalIdIsBlank),
      OrderTitle.from(title),
      validateOptionalNonNegativeDouble(priceAmount, OrderPriceIsNegative),
      validateOptionalText(priceCurrency, OrderPriceCurrencyIsBlank),
      Order.validateDateTimeRange(startDateTime, endDateTime),
      validateOptional(departureLocationId)(LocationId.from),
      validateOptional(arrivalLocationId)(LocationId.from)
    ).mapN {
      (
        validProviderId,
        validExternalOrderId,
        validTitle,
        validPriceAmount,
        validPriceCurrency,
        _,
        validDepartureLocationId,
        validArrivalLocationId
      ) =>
        OrderCreate(
          provider_id = validProviderId,
          service_type = serviceType,
          external_order_id = validExternalOrderId,
          title = validTitle,
          status = status,
          price_amount = validPriceAmount,
          price_currency = validPriceCurrency,
          start_datetime = startDateTime,
          end_datetime = endDateTime,
          departure_location_id = validDepartureLocationId,
          arrival_location_id = validArrivalLocationId
        )
    }

  def validate(order: OrderCreate): Result[OrderCreate] =
    from(
      providerId = order.provider_id.map(_.value),
      serviceType = order.service_type,
      externalOrderId = order.external_order_id,
      title = OrderTitle.unwrap(order.title),
      status = order.status,
      priceAmount = order.price_amount,
      priceCurrency = order.price_currency,
      startDateTime = order.start_datetime,
      endDateTime = order.end_datetime,
      departureLocationId = order.departure_location_id.map(_.value),
      arrivalLocationId = order.arrival_location_id.map(_.value)
    )

final case class OrderUpdate(
  provider_id: Option[ProviderId] = None,
  service_type: Option[ServiceType] = None,
  external_order_id: Option[String] = None,
  title: Option[OrderTitle] = None,
  status: Option[OrderStatus] = None,
  price_amount: Option[Double] = None,
  price_currency: Option[String] = None,
  start_datetime: Option[OffsetDateTime] = None,
  end_datetime: Option[OffsetDateTime] = None,
  departure_location_id: Option[LocationId] = None,
  arrival_location_id: Option[LocationId] = None
)

object OrderUpdate:

  def validate(order: OrderUpdate): Result[OrderUpdate] =
    (
      validateOptional(order.provider_id)(providerId => ProviderId.from(providerId.value)),
      validateOptionalText(order.external_order_id, OrderExternalIdIsBlank),
      validateOptional(order.title)(title => OrderTitle.from(OrderTitle.unwrap(title))),
      validateOptionalNonNegativeDouble(order.price_amount, OrderPriceIsNegative),
      validateOptionalText(order.price_currency, OrderPriceCurrencyIsBlank),
      Order.validateDateTimeRange(order.start_datetime, order.end_datetime),
      validateOptional(order.departure_location_id)(locationId => LocationId.from(locationId.value)),
      validateOptional(order.arrival_location_id)(locationId => LocationId.from(locationId.value))
    ).mapN {
      (
        validProviderId,
        validExternalOrderId,
        validTitle,
        validPriceAmount,
        validPriceCurrency,
        _,
        validDepartureLocationId,
        validArrivalLocationId
      ) =>
        order.copy(
          provider_id = validProviderId,
          external_order_id = validExternalOrderId,
          title = validTitle,
          price_amount = validPriceAmount,
          price_currency = validPriceCurrency,
          departure_location_id = validDepartureLocationId,
          arrival_location_id = validArrivalLocationId
        )
    }

final case class OrderStatusUpdate(
  status: OrderStatus,
  reason: Option[String] = None,
  new_start_datetime: Option[OffsetDateTime] = None,
  payload: Option[Json] = None
)

object OrderStatusUpdate:

  def validate(update: OrderStatusUpdate): Result[OrderStatusUpdate] =
    validateOptionalText(update.reason, OrderStatusReasonIsBlank).map { validReason =>
      update.copy(reason = validReason)
    }

final case class OrderSearchParams(
  serviceType: Option[ServiceType] = None,
  status: Option[OrderStatus] = None,
  fromDate: Option[LocalDate] = None,
  toDate: Option[LocalDate] = None
)

final case class OrderStatusEvent(
  id: OrderStatusEventId,
  order_id: OrderId,
  old_status: Option[OrderStatus],
  new_status: OrderStatus,
  reason: Option[String],
  payload: Option[Json],
  source: OrderStatusEventSource,
  created_at: Instant
)

final case class OrderFile(
  id: OrderFileId,
  order_id: OrderId,
  file_url: OrderFileUrl,
  file_type: FileType,
  parsed_data: Option[Json],
  uploaded_at: Instant
)

final case class OrderFileCreate(
  file_url: OrderFileUrl,
  file_type: FileType,
  parsed_data: Option[Json] = None
)

object OrderFileCreate:

  def from(
    fileUrl: String,
    fileType: FileType,
    parsedData: Option[Json] = None
  ): Result[OrderFileCreate] =
    OrderFileUrl.from(fileUrl).map { validFileUrl =>
      OrderFileCreate(validFileUrl, fileType, parsedData)
    }

  def validate(file: OrderFileCreate): Result[OrderFileCreate] =
    from(OrderFileUrl.unwrap(file.file_url), file.file_type, file.parsed_data)
