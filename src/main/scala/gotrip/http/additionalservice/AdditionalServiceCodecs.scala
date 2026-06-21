package gotrip.http.additionalservice

import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.http.ApiError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}

object AdditionalServiceCodecs:
  // API errors
  given Encoder[ApiError] =
    deriveEncoder

  given Decoder[ApiError] =
    deriveDecoder

  given Schema[ApiError] =
    derived

  // ServiceId
  given Encoder[ServiceId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[ServiceId] =
    Decoder.decodeLong.map(ServiceId.apply)

  given Schema[ServiceId] =
    Schema.schemaForLong.map(value => Some(ServiceId(value)))(_.value)

  given Codec[String, ServiceId, CodecFormat.TextPlain] =
    Codec.long.map(ServiceId.apply)(_.value)

  // ServiceTitle
  given Encoder[ServiceTitle] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[ServiceTitle] =
    Decoder.decodeString.map(ServiceTitle.apply)

  given Schema[ServiceTitle] =
    Schema.schemaForString.map(value => Some(ServiceTitle(value)))(_.value)

  // ProviderId
  given Encoder[ProviderId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[ProviderId] =
    Decoder.decodeLong.map(ProviderId.apply)

  given Schema[ProviderId] =
    Schema.schemaForLong.map(value => Some(ProviderId(value)))(_.value)

  given Codec[String, ProviderId, CodecFormat.TextPlain] =
    Codec.long.map(ProviderId.apply)(_.value)

  // LocationId
  given Encoder[LocationId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[LocationId] =
    Decoder.decodeLong.map(LocationId.apply)

  given Schema[LocationId] =
    Schema.schemaForLong.map(value => Some(LocationId(value)))(_.value)

  given Codec[String, LocationId, CodecFormat.TextPlain] =
    Codec.long.map(LocationId.apply)(_.value)

  // ServiceType
  given Encoder[ServiceType] =
    Encoder.encodeString.contramap(encodeServiceType)

  given Decoder[ServiceType] =
    Decoder.decodeString.emap { value =>
      parseServiceType(value).left.map(_.message)
    }

  given Schema[ServiceType] =
    Schema.schemaForString.map(value => parseServiceType(value).toOption)(encodeServiceType)

  given Codec[String, ServiceType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseServiceType(value) match
        case Right(serviceType) =>
          sttp.tapir.DecodeResult.Value(serviceType)
        case Left(error) =>
          sttp.tapir.DecodeResult.Error(value, new Exception(error.message))
    }(encodeServiceType)

  // Additional service models
  given Encoder[AdditionalService] =
    deriveEncoder

  given Decoder[AdditionalService] =
    deriveDecoder

  given Schema[AdditionalService] =
    derived

  given Encoder[AdditionalServiceCreate] =
    deriveEncoder

  given Decoder[AdditionalServiceCreate] =
    deriveDecoder

  given Schema[AdditionalServiceCreate] =
    derived

  given Encoder[AdditionalServiceUpdate] =
    deriveEncoder

  given Decoder[AdditionalServiceUpdate] =
    deriveDecoder

  given Schema[AdditionalServiceUpdate] =
    derived

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
