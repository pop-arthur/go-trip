package gotrip.http.userachievement

import gotrip.domain.userachievement._
import gotrip.domain.achievement.AchievementId
import gotrip.domain.user.UserId
import gotrip.http.HttpError
import gotrip.http.user.UserCodecs.given
import gotrip.http.achievement.AchievementCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}

object UserAchievementCodecs:

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[UserAchievement] = deriveEncoder
  given Decoder[UserAchievement] = deriveDecoder
  given Schema[UserAchievement] = derived

  given Encoder[UserAchievementId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[UserAchievementId] = Decoder.decodeLong.map(UserAchievementId.apply)
  given Schema[UserAchievementId] =
    Schema.schemaForLong
      .map(value => Some(UserAchievementId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[UserAchievementId](_.value))

  given Codec[String, UserAchievementId, CodecFormat.TextPlain] =
    Codec.long.map(UserAchievementId.apply)(_.value)