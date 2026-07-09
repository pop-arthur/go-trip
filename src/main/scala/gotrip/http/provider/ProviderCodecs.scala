package gotrip.http.provider

import gotrip.domain.provider.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}

object ProviderCodecs:
  given Encoder[ApiError] =
    deriveEncoder

  given Decoder[ApiError] =
    deriveDecoder

  given Schema[ApiError] =
    derived

  given Encoder[ProviderId] =
    uuidEncoder(_.value)

  given Decoder[ProviderId] =
    uuidDecoder(ProviderId.apply)

  given Schema[ProviderId] =
    uuidSchema(ProviderId.apply, _.value)

  given Codec[String, ProviderId, CodecFormat.TextPlain] =
    uuidTextCodec(ProviderId.apply, _.value)

  given Encoder[ProviderName] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[ProviderName] =
    Decoder.decodeString.map(ProviderName.apply)

  given Schema[ProviderName] =
    Schema.schemaForString.map(value => Some(ProviderName(value)))(_.value)

  given Encoder[ProviderType] =
    Encoder.encodeString.contramap(encodeProviderType)

  given Decoder[ProviderType] =
    Decoder.decodeString.emap { value =>
      parseProviderType(value).left.map(_.message)
    }

  given Schema[ProviderType] =
    Schema.schemaForString.map(value => parseProviderType(value).toOption)(encodeProviderType)

  given Codec[String, ProviderType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseProviderType(value) match
        case Right(providerType) =>
          sttp.tapir.DecodeResult.Value(providerType)
        case Left(error) =>
          sttp.tapir.DecodeResult.Error(value, new Exception(error.message))
    }(encodeProviderType)

  given Encoder[Provider] =
    deriveEncoder

  given Decoder[Provider] =
    deriveDecoder

  given Schema[Provider] =
    derived

  given Encoder[ProviderCreate] =
    deriveEncoder

  given Decoder[ProviderCreate] =
    deriveDecoder

  given Schema[ProviderCreate] =
    derived

  given Encoder[ProviderUpdate] =
    deriveEncoder

  given Decoder[ProviderUpdate] =
    deriveDecoder

  given Schema[ProviderUpdate] =
    derived

  private def parseProviderType(value: String): Either[ApiError, ProviderType] =
    value.toUpperCase match
      case "AIRLINE"             => Right(ProviderType.Airline)
      case "HOTEL"               => Right(ProviderType.Hotel)
      case "TOUR_COMPANY"        => Right(ProviderType.TourCompany)
      case "TRANSPORT_COMPANY"   => Right(ProviderType.TransportCompany)
      case "BOOKING_PLATFORM"    => Right(ProviderType.BookingPlatform)
      case "INSURANCE_COMPANY"   => Right(ProviderType.InsuranceCompany)
      case "OTHER"               => Right(ProviderType.Other)
      case other                 => Left(ApiError("VALIDATION_ERROR", s"Unknown provider type: $other"))

  private def encodeProviderType(providerType: ProviderType): String =
    providerType match
      case ProviderType.Airline          => "AIRLINE"
      case ProviderType.Hotel            => "HOTEL"
      case ProviderType.TourCompany      => "TOUR_COMPANY"
      case ProviderType.TransportCompany => "TRANSPORT_COMPANY"
      case ProviderType.BookingPlatform  => "BOOKING_PLATFORM"
      case ProviderType.InsuranceCompany => "INSURANCE_COMPANY"
      case ProviderType.Other            => "OTHER"
