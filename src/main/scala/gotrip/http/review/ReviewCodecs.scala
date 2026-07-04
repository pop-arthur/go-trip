package gotrip.http.review

import gotrip.domain.review._
import gotrip.domain.user.UserId
import gotrip.http.UuidCodecs.*
import gotrip.http.HttpError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}
import gotrip.http.user.UserCodecs.given

object ReviewCodecs:

  given Encoder[HttpError.Validation] = deriveEncoder
  given Decoder[HttpError.Validation] = deriveDecoder
  given Schema[HttpError.Validation] = derived

  given Encoder[HttpError.NotFound] = deriveEncoder
  given Decoder[HttpError.NotFound] = deriveDecoder
  given Schema[HttpError.NotFound] = derived

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[ReviewId] = uuidEncoder(_.value)
  given Decoder[ReviewId] = uuidDecoder(ReviewId.apply)
  given Schema[ReviewId] =
    uuidSchema(ReviewId.apply, _.value)

  given Codec[String, ReviewId, CodecFormat.TextPlain] =
    uuidTextCodec(ReviewId.apply, _.value)

  given Encoder[ReviewTargetId] = uuidEncoder(_.value)
  given Decoder[ReviewTargetId] = uuidDecoder(ReviewTargetId.apply)
  given Schema[ReviewTargetId] =
    uuidSchema(ReviewTargetId.apply, _.value)

  given Codec[String, ReviewTargetId, CodecFormat.TextPlain] =
    uuidTextCodec(ReviewTargetId.apply, _.value)

  given Encoder[ReviewTargetType] = Encoder.encodeString.contramap(_.toString)
  given Decoder[ReviewTargetType] = Decoder.decodeString.emap { s =>
    ReviewTargetType.fromString(s).toRight(s"Invalid review target type: $s")
  }
  given Schema[ReviewTargetType] = derived

  given Codec[String, ReviewTargetType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      ReviewTargetType.fromString(s) match
        case Some(v) => sttp.tapir.DecodeResult.Value(v)
        case None    => sttp.tapir.DecodeResult.Error(s, new Exception(s"Invalid review target type: $s"))
    }(_.toString)

  given Encoder[ReviewRating] = Encoder.encodeInt.contramap(_.value)
  given Decoder[ReviewRating] = Decoder.decodeInt.map(ReviewRating.apply)
  given Schema[ReviewRating] =
    Schema.schemaForInt
      .map(value => Some(ReviewRating(value)))(_.value)
      .validate(Validator.inRange(1, 5).contramap[ReviewRating](_.value))

  given Encoder[ReviewText] = Encoder.encodeOption[String].contramap(_.value)
  given Decoder[ReviewText] = Decoder.decodeOption[String].map(ReviewText.apply)
  given Schema[ReviewText] =
    Schema.schemaForOption[String].map(value => Some(ReviewText(value)))(_.value)

  given Encoder[Review] = deriveEncoder
  given Decoder[Review] = deriveDecoder
  given Schema[Review] = derived

  case class ReviewRatingSummary(
    targetType: ReviewTargetType,
    targetId: ReviewTargetId,
    averageRating: Option[Double],
    reviewCount: Int
  )
  given Encoder[ReviewRatingSummary] = deriveEncoder
  given Decoder[ReviewRatingSummary] = deriveDecoder
  given Schema[ReviewRatingSummary] = derived

  case class ReviewCreateRequest(
    targetType: ReviewTargetType,
    targetId: ReviewTargetId,
    rating: Int,
    text: Option[String]
  )
  given Encoder[ReviewCreateRequest] = deriveEncoder
  given Decoder[ReviewCreateRequest] = deriveDecoder
  given Schema[ReviewCreateRequest] = derived

  case class ReviewUpdateRequest(
    rating: Option[Int],
    text: Option[String]
  )
  given Encoder[ReviewUpdateRequest] = deriveEncoder
  given Decoder[ReviewUpdateRequest] = deriveDecoder
  given Schema[ReviewUpdateRequest] = derived
