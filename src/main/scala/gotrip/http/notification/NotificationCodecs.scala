package gotrip.http.notification

import gotrip.domain.notification._
import gotrip.http.HttpError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}

object NotificationCodecs:

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[NotificationId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[NotificationId] = Decoder.decodeLong.map(NotificationId.apply)
  given Schema[NotificationId] =
    Schema.schemaForLong
      .map(value => Some(NotificationId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[NotificationId](_.value))

  given Codec[String, NotificationId, CodecFormat.TextPlain] =
    Codec.long.map(NotificationId.apply)(_.value)

  given Encoder[NotificationUserId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[NotificationUserId] = Decoder.decodeLong.map(NotificationUserId.apply)
  given Schema[NotificationUserId] =
    Schema.schemaForLong
      .map(value => Some(NotificationUserId(value)))(_.value)
      .validate(Validator.positive[Long].contramap[NotificationUserId](_.value))

  given Codec[String, NotificationUserId, CodecFormat.TextPlain] =
    Codec.long.map(NotificationUserId.apply)(_.value)

  given Encoder[NotificationOrderId] = Encoder.encodeOption[Long].contramap(_.value)
  given Decoder[NotificationOrderId] = Decoder.decodeOption[Long].map(NotificationOrderId.apply)
  given Schema[NotificationOrderId] =
    Schema.schemaForOption[Long].map(value => Some(NotificationOrderId(value)))(_.value)

  given Encoder[NotificationTitle] = Encoder.encodeString.contramap(_.value)
  given Decoder[NotificationTitle] = Decoder.decodeString.map(NotificationTitle.apply)
  given Schema[NotificationTitle] =
    Schema.schemaForString.map(value => Some(NotificationTitle(value)))(_.value)

  given Encoder[NotificationBody] = Encoder.encodeString.contramap(_.value)
  given Decoder[NotificationBody] = Decoder.decodeString.map(NotificationBody.apply)
  given Schema[NotificationBody] =
    Schema.schemaForString.map(value => Some(NotificationBody(value)))(_.value)

  given Encoder[NotificationType] =
    Encoder.encodeString.contramap(NotificationType.toString)

  given Decoder[NotificationType] =
    Decoder.decodeString.emap { value =>
      NotificationType.fromString(value).toRight(s"Invalid notification type: $value")
    }

  given Schema[NotificationType] = derived

  given Codec[String, NotificationType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      NotificationType.fromString(value) match
        case Some(notificationType) => DecodeResult.Value(notificationType)
        case None =>
          DecodeResult.Error(value, new Exception(s"Invalid notification type: $value"))
    }(NotificationType.toString)

  given Encoder[UserNotification] =
    Encoder.forProduct10(
      "id",
      "user_id",
      "order_id",
      "type",
      "title",
      "body",
      "is_read",
      "sent_at",
      "created_at",
      "updated_at"
    ) { notification =>
      (
        notification.id,
        notification.userId,
        notification.orderId,
        notification.notificationType,
        notification.title,
        notification.body,
        notification.isRead,
        notification.sentAt,
        notification.createdAt,
        notification.updatedAt
      )
    }

  given Decoder[UserNotification] =
    Decoder.forProduct10(
      "id",
      "user_id",
      "order_id",
      "type",
      "title",
      "body",
      "is_read",
      "sent_at",
      "created_at",
      "updated_at"
    )(UserNotification.apply)

  given Schema[UserNotification] = derived
