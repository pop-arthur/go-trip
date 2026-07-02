package gotrip.http.userachievement

import gotrip.domain.userachievement._
import gotrip.domain.achievement.AchievementId
import gotrip.domain.user.UserId
import gotrip.http.HttpError
import gotrip.http.UuidCodecs.*
import gotrip.http.user.UserCodecs.given
import gotrip.http.achievement.AchievementCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}

object UserAchievementCodecs:

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[UserAchievement] = deriveEncoder
  given Decoder[UserAchievement] = deriveDecoder
  given Schema[UserAchievement] = derived

  given Encoder[UserAchievementId] = uuidEncoder(_.value)
  given Decoder[UserAchievementId] = uuidDecoder(UserAchievementId.apply)
  given Schema[UserAchievementId] =
    uuidSchema(UserAchievementId.apply, _.value)

  given Codec[String, UserAchievementId, CodecFormat.TextPlain] =
    uuidTextCodec(UserAchievementId.apply, _.value)
