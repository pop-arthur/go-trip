package gotrip.http.triplocation

import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
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
    uuidEncoder(_.value)

  given Decoder[LocationId] =
    uuidDecoder(LocationId.apply)

  given Schema[LocationId] =
    uuidSchema(LocationId.apply, _.value)

  // TripId
  given Encoder[TripId] =
    uuidEncoder(_.value)

  given Decoder[TripId] =
    uuidDecoder(TripId.apply)

  given Schema[TripId] =
    uuidSchema(TripId.apply, _.value)

  given Codec[String, TripId, CodecFormat.TextPlain] =
    uuidTextCodec(TripId.apply, _.value)

  // TripLocationId
  given Encoder[TripLocationId] =
    uuidEncoder(_.value)

  given Decoder[TripLocationId] =
    uuidDecoder(TripLocationId.apply)

  given Schema[TripLocationId] =
    uuidSchema(TripLocationId.apply, _.value)

  given Codec[String, TripLocationId, CodecFormat.TextPlain] =
    uuidTextCodec(TripLocationId.apply, _.value)

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
