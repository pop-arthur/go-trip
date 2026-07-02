package gotrip.http.achievement

import gotrip.domain.achievement._
import gotrip.http.HttpError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}

object AchievementCodecs {

  given Encoder[HttpError.Validation] = deriveEncoder
  given Decoder[HttpError.Validation] = deriveDecoder
  given Schema[HttpError.Validation] = derived

  given Encoder[HttpError.NotFound] = deriveEncoder
  given Decoder[HttpError.NotFound] = deriveDecoder
  given Schema[HttpError.NotFound] = derived

  given Encoder[HttpError.Conflict] = deriveEncoder
  given Decoder[HttpError.Conflict] = deriveDecoder
  given Schema[HttpError.Conflict] = derived

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[Achievement] = deriveEncoder
  given Decoder[Achievement] = deriveDecoder
  given Schema[Achievement] = derived

  given Encoder[AchievementId] = uuidEncoder(_.value)
  given Decoder[AchievementId] = uuidDecoder(AchievementId.apply)
  given Schema[AchievementId] =
    uuidSchema(AchievementId.apply, _.value)

  given Codec[String, AchievementId, CodecFormat.TextPlain] =
    uuidTextCodec(AchievementId.apply, _.value)

  given Encoder[AchievementCode] = Encoder.encodeString.contramap(_.value)
  given Decoder[AchievementCode] = Decoder.decodeString.map(AchievementCode.apply)
  given Schema[AchievementCode] =
    Schema.schemaForString.map(value => Some(AchievementCode(value)))(_.value)

  given Encoder[AchievementTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[AchievementTitle] = Decoder.decodeString.map(AchievementTitle.apply)
  given Schema[AchievementTitle] =
    Schema.schemaForString.map(value => Some(AchievementTitle(value)))(_.value)

  given Encoder[AchievementDescription] =
    Encoder.encodeOption[String].contramap(_.value)
  given Decoder[AchievementDescription] =
    Decoder.decodeOption[String].map(AchievementDescription.apply)
  given Schema[AchievementDescription] =
    Schema.schemaForOption[String].map(value => Some(AchievementDescription(value)))(_.value)

  given Encoder[AchievementIconUrl] =
    Encoder.encodeOption[String].contramap(_.value)
  given Decoder[AchievementIconUrl] =
    Decoder.decodeOption[String].map(AchievementIconUrl.apply)
  given Schema[AchievementIconUrl] =
    Schema.schemaForOption[String].map(value => Some(AchievementIconUrl(value)))(_.value)

  given Encoder[AchievementConditionType] =
    Encoder.encodeString.contramap(_.toString)
  given Decoder[AchievementConditionType] =
    Decoder.decodeString.emap { s =>
      AchievementConditionType.fromString(s).toRight(s"Invalid achievement condition type: $s")
    }
  given Schema[AchievementConditionType] = derived

  given Codec[String, AchievementConditionType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      AchievementConditionType.fromString(s) match
        case Some(v) => sttp.tapir.DecodeResult.Value(v)
        case None    => sttp.tapir.DecodeResult.Error(s, new Exception(s"Invalid achievement condition type: $s"))
    }(_.toString)

  case class AchievementCreateRequest(
    code: String,
    title: String,
    description: Option[String],
    conditionType: AchievementConditionType,
    conditionValue: Int,
    iconUrl: Option[String]
  )
  given Encoder[AchievementCreateRequest] = deriveEncoder
  given Decoder[AchievementCreateRequest] = deriveDecoder
  given Schema[AchievementCreateRequest] = derived

  case class AchievementUpdateRequest(
    code: Option[String],
    title: Option[String],
    description: Option[String],
    conditionType: Option[AchievementConditionType],
    conditionValue: Option[Int],
    iconUrl: Option[String]
  )
  given Encoder[AchievementUpdateRequest] = deriveEncoder
  given Decoder[AchievementUpdateRequest] = deriveDecoder
  given Schema[AchievementUpdateRequest] = derived
}
