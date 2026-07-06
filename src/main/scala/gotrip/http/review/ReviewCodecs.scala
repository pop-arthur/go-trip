package gotrip.http.review

import gotrip.domain.review._
import gotrip.domain.user.UserId
import gotrip.http.HttpError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}
import java.util.UUID

import gotrip.http.user.UserCodecs.{given_Encoder_UserId, given_Decoder_UserId, given_Schema_UserId}

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

  given Encoder[ReviewId] = Encoder.encodeUUID.contramap(id => gotrip.domain.review.value(id))
  given Decoder[ReviewId] = Decoder.decodeUUID.map(ReviewId.apply)
  given Schema[ReviewId] = Schema.schemaForUUID.map(value => Some(ReviewId(value)))(id => gotrip.domain.review.value(id))

  given Codec[String, ReviewId, CodecFormat.TextPlain] =
    Codec.uuid.map(ReviewId.apply)(id => gotrip.domain.review.value(id))

  given Encoder[ReviewTargetType] = Encoder.encodeString.contramap(_.toString)
  given Decoder[ReviewTargetType] = Decoder.decodeString.emap { s =>
    ReviewTargetType.fromString(s).toRight(s"Invalid review target type: $s")
  }
  given Schema[ReviewTargetType] = derived

  given Codec[String, ReviewTargetType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      ReviewTargetType.fromString(s) match
        case Some(v) => DecodeResult.Value(v)
        case None    => DecodeResult.Error(s, new Exception(s"Invalid review target type: $s"))
    }(_.toString)

  given Encoder[ReviewTargetId] = Encoder.encodeUUID.contramap(id => gotrip.domain.review.value(id))
  given Decoder[ReviewTargetId] = Decoder.decodeUUID.map(ReviewTargetId.apply)
  given Schema[ReviewTargetId] = Schema.schemaForUUID.map(value => Some(ReviewTargetId(value)))(id => gotrip.domain.review.value(id))

  given Codec[String, ReviewTargetId, CodecFormat.TextPlain] =
    Codec.uuid.map(ReviewTargetId.apply)(id => gotrip.domain.review.value(id))

  given Encoder[ReviewRating] = Encoder.encodeInt.contramap(r => gotrip.domain.review.value(r))
  given Decoder[ReviewRating] = Decoder.decodeInt.map(ReviewRating.apply)
  given Schema[ReviewRating] =
    Schema.schemaForInt
      .map(value => Some(ReviewRating(value)))(r => gotrip.domain.review.value(r))
      .validate(Validator.inRange(1, 5).contramap[ReviewRating](r => gotrip.domain.review.value(r)))

  given Encoder[ReviewText] = Encoder.encodeOption[String].contramap(t => gotrip.domain.review.value(t))
  given Decoder[ReviewText] = Decoder.decodeOption[String].map(ReviewText.apply)
  given Schema[ReviewText] =
    Schema.schemaForOption[String].map(value => Some(ReviewText(value)))(t => gotrip.domain.review.value(t))
