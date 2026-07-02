package gotrip.http.additionalservice

import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}

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
    uuidEncoder(_.value)

  given Decoder[ServiceId] =
    uuidDecoder(ServiceId.apply)

  given Schema[ServiceId] =
    uuidSchema(ServiceId.apply, _.value)

  given Codec[String, ServiceId, CodecFormat.TextPlain] =
    uuidTextCodec(ServiceId.apply, _.value)

  // ServiceTitle
  given Encoder[ServiceTitle] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[ServiceTitle] =
    Decoder.decodeString.map(ServiceTitle.apply)

  given Schema[ServiceTitle] =
    Schema.schemaForString.map(value => Some(ServiceTitle(value)))(_.value)

  // ProviderId
  given Encoder[ProviderId] =
    uuidEncoder(_.value)

  given Decoder[ProviderId] =
    uuidDecoder(ProviderId.apply)

  given Schema[ProviderId] =
    uuidSchema(ProviderId.apply, _.value)

  given Codec[String, ProviderId, CodecFormat.TextPlain] =
    uuidTextCodec(ProviderId.apply, _.value)

  // LocationId
  given Encoder[LocationId] =
    uuidEncoder(_.value)

  given Decoder[LocationId] =
    uuidDecoder(LocationId.apply)

  given Schema[LocationId] =
    uuidSchema(LocationId.apply, _.value)

  given Codec[String, LocationId, CodecFormat.TextPlain] =
    uuidTextCodec(LocationId.apply, _.value)

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
    derived[AdditionalServiceCreate]
      .modifyUnsafe[Double]("price_amount", Schema.ModifyCollectionElements)(
        _.validate(Validator.positiveOrZero[Double])
      )

  given Encoder[AdditionalServiceUpdate] =
    deriveEncoder

  given Decoder[AdditionalServiceUpdate] =
    deriveDecoder

  given Schema[AdditionalServiceUpdate] =
    derived[AdditionalServiceUpdate]
      .modifyUnsafe[Double]("price_amount", Schema.ModifyCollectionElements)(
        _.validate(Validator.positiveOrZero[Double])
      )

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
