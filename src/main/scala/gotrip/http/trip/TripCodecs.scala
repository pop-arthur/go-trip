package gotrip.http.trip

import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter
import scala.util.Try

object TripCodecs:
  given Encoder[ApiError] = deriveEncoder
  given Decoder[ApiError] = deriveDecoder
  given Schema[ApiError] = derived

  given Encoder[Instant] =
    Encoder.encodeString.contramap(_.toString)

  given Decoder[Instant] =
    Decoder.decodeString.emap(value => Try(Instant.parse(value)).toEither.left.map(_.getMessage))

  given Schema[Instant] =
    Schema.schemaForString.map(value => Try(Instant.parse(value)).toOption)(_.toString)

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

  given Encoder[UserId] =
    uuidEncoder(_.value)

  given Decoder[UserId] =
    uuidDecoder(UserId.apply)

  given Schema[UserId] =
    uuidSchema(UserId.apply, _.value)

  given Encoder[TripId] =
    uuidEncoder(_.value)

  given Decoder[TripId] =
    uuidDecoder(TripId.apply)

  given Schema[TripId] =
    uuidSchema(TripId.apply, _.value)

  given Codec[String, TripId, CodecFormat.TextPlain] =
    uuidTextCodec(TripId.apply, _.value)

  given Encoder[TripTitle] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[TripTitle] =
    Decoder.decodeString.map(TripTitle.apply)

  given Schema[TripTitle] =
    Schema.schemaForString.map(value => Some(TripTitle(value)))(_.value)

  given Encoder[TripStartDate] =
    Encoder.encodeOption[LocalDate].contramap(_.value)

  given Decoder[TripStartDate] =
    Decoder.decodeOption[LocalDate].map(TripStartDate.apply)

  given Schema[TripStartDate] =
    Schema.schemaForOption[LocalDate].map(value => Some(TripStartDate(value)))(_.value)

  given Encoder[TripEndDate] =
    Encoder.encodeOption[LocalDate].contramap(_.value)

  given Decoder[TripEndDate] =
    Decoder.decodeOption[LocalDate].map(TripEndDate.apply)

  given Schema[TripEndDate] =
    Schema.schemaForOption[LocalDate].map(value => Some(TripEndDate(value)))(_.value)

  given Encoder[TripStatus] =
    Encoder.encodeString.contramap(encodeTripStatus)

  given Decoder[TripStatus] =
    Decoder.decodeString.emap(value => parseTripStatus(value).left.map(_.message))

  given Schema[TripStatus] =
    Schema.schemaForString.map(value => parseTripStatus(value).toOption)(encodeTripStatus)

  given Codec[String, TripStatus, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseTripStatus(value) match
        case Right(status) => DecodeResult.Value(status)
        case Left(error)   => DecodeResult.Error(value, new Exception(error.message))
    }(encodeTripStatus)

  given Encoder[Trip] = deriveEncoder
  given Decoder[Trip] = deriveDecoder
  given Schema[Trip] = derived

  given Encoder[TripCreate] = deriveEncoder
  given Decoder[TripCreate] = deriveDecoder
  given Schema[TripCreate] = derived

  given Encoder[TripUpdate] = deriveEncoder
  given Decoder[TripUpdate] = deriveDecoder
  given Schema[TripUpdate] = derived

  private def parseTripStatus(value: String): Either[ApiError, TripStatus] =
    value.toUpperCase match
      case "PLANNED"   => Right(TripStatus.Planned)
      case "ACTIVE"    => Right(TripStatus.Active)
      case "COMPLETED" => Right(TripStatus.Completed)
      case "CANCELLED" => Right(TripStatus.Cancelled)
      case other       => Left(ApiError("VALIDATION_ERROR", s"Unknown trip status: $other"))

  private def encodeTripStatus(status: TripStatus): String =
    status match
      case TripStatus.Planned   => "PLANNED"
      case TripStatus.Active    => "ACTIVE"
      case TripStatus.Completed => "COMPLETED"
      case TripStatus.Cancelled => "CANCELLED"
