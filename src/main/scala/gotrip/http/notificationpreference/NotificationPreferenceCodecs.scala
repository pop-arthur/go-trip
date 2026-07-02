package gotrip.http.notificationpreference

import gotrip.domain.notificationpreference._
import gotrip.http.HttpError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}
import gotrip.http.user.UserCodecs.given

object NotificationPreferenceCodecs:

  given Encoder[HttpError.Validation] = deriveEncoder
  given Decoder[HttpError.Validation] = deriveDecoder
  given Schema[HttpError.Validation] = derived

  given Encoder[HttpError.NotFound] = deriveEncoder
  given Decoder[HttpError.NotFound] = deriveDecoder
  given Schema[HttpError.NotFound] = derived

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[NotificationPreference] = deriveEncoder
  given Decoder[NotificationPreference] = deriveDecoder
  given Schema[NotificationPreference] = derived

  given Encoder[NotificationPreferenceId] = uuidEncoder(_.value)
  given Decoder[NotificationPreferenceId] = uuidDecoder(NotificationPreferenceId.apply)
  given Schema[NotificationPreferenceId] =
    uuidSchema(NotificationPreferenceId.apply, _.value)

  given Codec[String, NotificationPreferenceId, CodecFormat.TextPlain] =
    uuidTextCodec(NotificationPreferenceId.apply, _.value)

  case class NotificationPreferenceUpdate(isEnabled: Boolean)
  given Encoder[NotificationPreferenceUpdate] = deriveEncoder
  given Decoder[NotificationPreferenceUpdate] = deriveDecoder
  given Schema[NotificationPreferenceUpdate] = derived
