package gotrip.http.triplocation

import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.http.ApiError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

object TripLocationCodecs:
  // API errors
  given Encoder[ApiError] =
    deriveEncoder

  given Decoder[ApiError] =
    deriveDecoder

  given Schema[ApiError] =
    derived

  // Date-time values
  given Encoder[OffsetDateTime] =
    Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

  given Decoder[OffsetDateTime] =
    Decoder.decodeString.emap { value =>
      Try(OffsetDateTime.parse(value)).toEither.left.map(_.getMessage)
    }

  given Schema[OffsetDateTime] =
    Schema.schemaForString.map(value => Try(OffsetDateTime.parse(value)).toOption)(
      _.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )

  // LocationId
  given Encoder[LocationId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[LocationId] =
    Decoder.decodeLong.map(LocationId.apply)

  given Schema[LocationId] =
    Schema.schemaForLong
      .map(value => Some(LocationId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[LocationId](_.value))

  // TripId
  given Encoder[TripId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[TripId] =
    Decoder.decodeLong.map(TripId.apply)

  given Schema[TripId] =
    Schema.schemaForLong
      .map(value => Some(TripId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[TripId](_.value))

  given Codec[String, TripId, CodecFormat.TextPlain] =
    Codec.long
      .map(TripId.apply)(_.value)
      .validate(Validator.positive[Long].contramap[TripId](_.value))

  // TripLocationId
  given Encoder[TripLocationId] =
    Encoder.encodeLong.contramap(_.value)

  given Decoder[TripLocationId] =
    Decoder.decodeLong.map(TripLocationId.apply)

  given Schema[TripLocationId] =
    Schema.schemaForLong
      .map(value => Some(TripLocationId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[TripLocationId](_.value))

  given Codec[String, TripLocationId, CodecFormat.TextPlain] =
    Codec.long
      .map(TripLocationId.apply)(_.value)
      .validate(Validator.positive[Long].contramap[TripLocationId](_.value))

  // VisitOrder
  given Encoder[VisitOrder] =
    Encoder.encodeInt.contramap(_.value)

  given Decoder[VisitOrder] =
    Decoder.decodeInt.map(VisitOrder.apply)

  given Schema[VisitOrder] =
    Schema.schemaForInt
      .map(value => Some(VisitOrder(value)))(_.value)
      .validate(Validator.positive[Int].contramap[VisitOrder](_.value))

  // Optional date-time wrappers
  given Encoder[TripLocationArrivalDate] =
    Encoder.encodeOption[OffsetDateTime].contramap(_.value)

  given Decoder[TripLocationArrivalDate] =
    Decoder.decodeOption[OffsetDateTime].map(TripLocationArrivalDate.apply)

  given Schema[TripLocationArrivalDate] =
    Schema.schemaForOption[OffsetDateTime].map(value => Some(TripLocationArrivalDate(value)))(_.value)

  given Encoder[TripLocationDepartureDate] =
    Encoder.encodeOption[OffsetDateTime].contramap(_.value)

  given Decoder[TripLocationDepartureDate] =
    Decoder.decodeOption[OffsetDateTime].map(TripLocationDepartureDate.apply)

  given Schema[TripLocationDepartureDate] =
    Schema.schemaForOption[OffsetDateTime].map(value => Some(TripLocationDepartureDate(value)))(_.value)

  // Trip location models
  given Encoder[TripLocation] =
    deriveEncoder

  given Decoder[TripLocation] =
    deriveDecoder

  given Schema[TripLocation] =
    derived

  given Encoder[TripLocationCreate] =
    deriveEncoder

  given Decoder[TripLocationCreate] =
    deriveDecoder

  given Schema[TripLocationCreate] =
    derived

  given Encoder[TripLocationUpdate] =
    deriveEncoder

  given Decoder[TripLocationUpdate] =
    deriveDecoder

  given Schema[TripLocationUpdate] =
    derived
