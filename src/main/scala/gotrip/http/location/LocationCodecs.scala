package gotrip.http.location

import gotrip.domain.location.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.{Codec, CodecFormat, Schema}
import sttp.tapir.Schema.derived

object LocationCodecs:
  // API errors
  given Encoder[ApiError] =
    deriveEncoder

  given Decoder[ApiError] =
    deriveDecoder

  given Schema[ApiError] =
    derived

  // Location models
  given Encoder[Location] =
    deriveEncoder

  given Decoder[Location] =
    deriveDecoder

  given Schema[Location] =
    derived

  given Encoder[LocationCreate] =
    deriveEncoder

  given Decoder[LocationCreate] =
    deriveDecoder

  given Schema[LocationCreate] =
    derived

  given Encoder[LocationUpdate] =
    deriveEncoder

  given Decoder[LocationUpdate] =
    deriveDecoder

  given Schema[LocationUpdate] =
    derived
  
  // LocationId
  given Encoder[LocationId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[LocationId] =
    Decoder.decodeLong.map(LocationId.apply)

  given Schema[LocationId] =
    Schema.schemaForLong.map(value => Some(LocationId(value)))(_.value)

  given Codec[String, LocationId, CodecFormat.TextPlain] =
    Codec.long.map(LocationId.apply)(_.value)

  // LocationName
  given Encoder[LocationName] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[LocationName] =
    Decoder.decodeString.map(LocationName.apply)

  given Schema[LocationName] =
    Schema.schemaForString.map(value => Some(LocationName(value)))(_.value)

  // Optional text fields
  given Encoder[LocationCountry] =
    Encoder.encodeOption[String].contramap(_.value)

  given Decoder[LocationCountry] =
    Decoder.decodeOption[String].map(LocationCountry.apply)

  given Schema[LocationCountry] =
    Schema.schemaForOption[String].map(value => Some(LocationCountry(value)))(_.value)

  given Encoder[LocationCity] =
    Encoder.encodeOption[String].contramap(_.value)

  given Decoder[LocationCity] =
    Decoder.decodeOption[String].map(LocationCity.apply)

  given Schema[LocationCity] =
    Schema.schemaForOption[String].map(value => Some(LocationCity(value)))(_.value)

  given Encoder[LocationAddress] =
    Encoder.encodeOption[String].contramap(_.value)

  given Decoder[LocationAddress] =
    Decoder.decodeOption[String].map(LocationAddress.apply)

  given Schema[LocationAddress] =
    Schema.schemaForOption[String].map(value => Some(LocationAddress(value)))(_.value)

  // Coordinates
  given Encoder[LocationLatitude] =
    Encoder.encodeOption[Double].contramap(_.value)

  given Decoder[LocationLatitude] =
    Decoder.decodeOption[Double].map(LocationLatitude.apply)

  given Schema[LocationLatitude] =
    Schema.schemaForOption[Double].map(value => Some(LocationLatitude(value)))(_.value)

  given Encoder[LocationLongitude] =
    Encoder.encodeOption[Double].contramap(_.value)

  given Decoder[LocationLongitude] =
    Decoder.decodeOption[Double].map(LocationLongitude.apply)

  given Schema[LocationLongitude] =
    Schema.schemaForOption[Double].map(value => Some(LocationLongitude(value)))(_.value)

  // LocationType
  given Encoder[LocationType] =
    Encoder.encodeString.contramap(encodeLocationType)

  given Decoder[LocationType] =
    Decoder.decodeString.emap { value =>
      parseLocationType(value).left.map(error => error.message)
    }

  given Schema[LocationType] =
    Schema.schemaForString.map(value => parseLocationType(value).toOption)(encodeLocationType)

  given Codec[String, LocationType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseLocationType(value) match
        case Right(locationType) =>
          sttp.tapir.DecodeResult.Value(locationType)
        case Left(error) =>
          sttp.tapir.DecodeResult.Error(value, new Exception(error.message))
    }(encodeLocationType)

  private def parseLocationType(value: String): Either[ApiError, LocationType] =
    value.toUpperCase match
      case "COUNTRY"       => Right(LocationType.Country)
      case "CITY"          => Right(LocationType.City)
      case "AIRPORT"       => Right(LocationType.Airport)
      case "TRAIN_STATION" => Right(LocationType.TrainStation)
      case "BUS_STATION"   => Right(LocationType.BusStation)
      case "PORT"          => Right(LocationType.Port)
      case "HOTEL"         => Right(LocationType.Hotel)
      case "MEETING_POINT" => Right(LocationType.MeetingPoint)
      case "ATTRACTION"    => Right(LocationType.Attraction)
      case "OTHER"         => Right(LocationType.Other)
      case other =>
        Left(ApiError("VALIDATION_ERROR", s"Unknown location type: $other"))

  private def encodeLocationType(locationType: LocationType): String =
    locationType match
      case LocationType.Country      => "COUNTRY"
      case LocationType.City         => "CITY"
      case LocationType.Airport      => "AIRPORT"
      case LocationType.TrainStation => "TRAIN_STATION"
      case LocationType.BusStation   => "BUS_STATION"
      case LocationType.Port         => "PORT"
      case LocationType.Hotel        => "HOTEL"
      case LocationType.MeetingPoint => "MEETING_POINT"
      case LocationType.Attraction   => "ATTRACTION"
      case LocationType.Other        => "OTHER"
