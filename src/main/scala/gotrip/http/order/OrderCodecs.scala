package gotrip.http.order

import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}

import java.time.{Instant, LocalDate, OffsetDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Try

object OrderCodecs:
  given Encoder[ApiError] = deriveEncoder
  given Decoder[ApiError] = deriveDecoder
  given Schema[ApiError] = derived

  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Instant] = Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))
  given Schema[Instant] = Schema.schemaForString.map(value => Try(Instant.parse(value)).toOption)(_.toString)

  given Encoder[OffsetDateTime] =
    Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  given Decoder[OffsetDateTime] =
    Decoder.decodeString.emap(value => Try(OffsetDateTime.parse(value)).toEither.left.map(_.getMessage))
  given Schema[OffsetDateTime] =
    Schema.schemaForString.map(value => Try(OffsetDateTime.parse(value)).toOption)(
      _.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )

  given Encoder[LocalDate] =
    Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_DATE))
  given Decoder[LocalDate] =
    Decoder.decodeString.emap(value => Try(LocalDate.parse(value)).toEither.left.map(_.getMessage))
  given Schema[LocalDate] =
    Schema.schemaForString.map(value => Try(LocalDate.parse(value)).toOption)(
      _.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
  given Codec[String, LocalDate, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      Try(LocalDate.parse(value)).toEither match
        case Right(date) => DecodeResult.Value(date)
        case Left(error) => DecodeResult.Error(value, error)
    }(_.format(DateTimeFormatter.ISO_LOCAL_DATE))

  given Schema[Json] =
    Schema.schemaForString.map(value => parse(value).toOption)(_.noSpaces)

  given Encoder[UserId] = uuidEncoder(_.value)
  given Decoder[UserId] = uuidDecoder(UserId.apply)
  given Schema[UserId] =
    uuidSchema(UserId.apply, _.value)

  given Encoder[TripId] = uuidEncoder(_.value)
  given Decoder[TripId] = uuidDecoder(TripId.apply)
  given Schema[TripId] =
    uuidSchema(TripId.apply, _.value)
  given Codec[String, TripId, CodecFormat.TextPlain] =
    uuidTextCodec(TripId.apply, _.value)

  given Encoder[OrderId] = uuidEncoder(_.value)
  given Decoder[OrderId] = uuidDecoder(OrderId.apply)
  given Schema[OrderId] =
    uuidSchema(OrderId.apply, _.value)
  given Codec[String, OrderId, CodecFormat.TextPlain] =
    uuidTextCodec(OrderId.apply, _.value)

  given Encoder[ProviderId] = uuidEncoder(_.value)
  given Decoder[ProviderId] = uuidDecoder(ProviderId.apply)
  given Schema[ProviderId] =
    uuidSchema(ProviderId.apply, _.value)

  given Encoder[LocationId] = uuidEncoder(_.value)
  given Decoder[LocationId] = uuidDecoder(LocationId.apply)
  given Schema[LocationId] =
    uuidSchema(LocationId.apply, _.value)

  given Encoder[OrderTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[OrderTitle] = Decoder.decodeString.map(OrderTitle.apply)
  given Schema[OrderTitle] =
    Schema.schemaForString.map(value => Some(OrderTitle(value)))(_.value)

  given Encoder[ServiceType] = Encoder.encodeString.contramap(encodeServiceType)
  given Decoder[ServiceType] = Decoder.decodeString.emap(value => parseServiceType(value).left.map(_.message))
  given Schema[ServiceType] = Schema.schemaForString.map(value => parseServiceType(value).toOption)(encodeServiceType)
  given Codec[String, ServiceType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseServiceType(value) match
        case Right(serviceType) => DecodeResult.Value(serviceType)
        case Left(error)        => DecodeResult.Error(value, new Exception(error.message))
    }(encodeServiceType)

  given Encoder[OrderStatus] = Encoder.encodeString.contramap(encodeOrderStatus)
  given Decoder[OrderStatus] = Decoder.decodeString.emap(value => parseOrderStatus(value).left.map(_.message))
  given Schema[OrderStatus] = Schema.schemaForString.map(value => parseOrderStatus(value).toOption)(encodeOrderStatus)
  given Codec[String, OrderStatus, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseOrderStatus(value) match
        case Right(status) => DecodeResult.Value(status)
        case Left(error)   => DecodeResult.Error(value, new Exception(error.message))
    }(encodeOrderStatus)

  given Encoder[Order] = deriveEncoder
  given Decoder[Order] = deriveDecoder
  given Schema[Order] = derived

  given Encoder[OrderCreate] = deriveEncoder
  given Decoder[OrderCreate] = deriveDecoder
  given Schema[OrderCreate] = derived

  given Encoder[OrderUpdate] = deriveEncoder
  given Decoder[OrderUpdate] = deriveDecoder
  given Schema[OrderUpdate] = derived

  given Encoder[OrderStatusUpdate] = deriveEncoder
  given Decoder[OrderStatusUpdate] = deriveDecoder
  given Schema[OrderStatusUpdate] = derived

  private def parseServiceType(value: String): Either[ApiError, ServiceType] =
    value.toUpperCase match
      case "FLIGHT"        => Right(ServiceType.Flight)
      case "TRAIN"         => Right(ServiceType.Train)
      case "BUS"           => Right(ServiceType.Bus)
      case "HOTEL"         => Right(ServiceType.Hotel)
      case "TOUR"          => Right(ServiceType.Tour)
      case "CAR_RENTAL"    => Right(ServiceType.CarRental)
      case "INSURANCE"     => Right(ServiceType.Insurance)
      case "TAXI"          => Right(ServiceType.Taxi)
      case "ESIM"          => Right(ServiceType.Esim)
      case "LOUNGE"        => Right(ServiceType.Lounge)
      case "EXTRA_BAGGAGE" => Right(ServiceType.ExtraBaggage)
      case "OTHER"         => Right(ServiceType.Other)
      case other           => Left(ApiError("VALIDATION_ERROR", s"Unknown service type: $other"))

  private def encodeServiceType(serviceType: ServiceType): String =
    serviceType match
      case ServiceType.Flight       => "FLIGHT"
      case ServiceType.Train        => "TRAIN"
      case ServiceType.Bus          => "BUS"
      case ServiceType.Hotel        => "HOTEL"
      case ServiceType.Tour         => "TOUR"
      case ServiceType.CarRental    => "CAR_RENTAL"
      case ServiceType.Insurance    => "INSURANCE"
      case ServiceType.Taxi         => "TAXI"
      case ServiceType.Esim         => "ESIM"
      case ServiceType.Lounge       => "LOUNGE"
      case ServiceType.ExtraBaggage => "EXTRA_BAGGAGE"
      case ServiceType.Other        => "OTHER"

  private def parseOrderStatus(value: String): Either[ApiError, OrderStatus] =
    value.toUpperCase match
      case "PENDING_VERIFICATION" => Right(OrderStatus.PendingVerification)
      case "CONFIRMED"            => Right(OrderStatus.Confirmed)
      case "DELAYED"              => Right(OrderStatus.Delayed)
      case "CANCELLED"            => Right(OrderStatus.Cancelled)
      case "COMPLETED"            => Right(OrderStatus.Completed)
      case "REFUND_PENDING"       => Right(OrderStatus.RefundPending)
      case "REFUNDED"             => Right(OrderStatus.Refunded)
      case other                  => Left(ApiError("VALIDATION_ERROR", s"Unknown order status: $other"))

  private def encodeOrderStatus(status: OrderStatus): String =
    status match
      case OrderStatus.PendingVerification => "PENDING_VERIFICATION"
      case OrderStatus.Confirmed           => "CONFIRMED"
      case OrderStatus.Delayed             => "DELAYED"
      case OrderStatus.Cancelled           => "CANCELLED"
      case OrderStatus.Completed           => "COMPLETED"
      case OrderStatus.RefundPending       => "REFUND_PENDING"
      case OrderStatus.Refunded            => "REFUNDED"
