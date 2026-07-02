package gotrip.http.notification

import gotrip.domain.notification._
import gotrip.http.HttpError
import gotrip.http.UuidCodecs.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

import java.util.UUID
import scala.util.Try

object NotificationCodecs:

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[NotificationId] = uuidEncoder(_.value)
  given Decoder[NotificationId] = uuidDecoder(NotificationId.apply)
  given Schema[NotificationId] =
    uuidSchema(NotificationId.apply, _.value)

  given Codec[String, NotificationId, CodecFormat.TextPlain] =
    uuidTextCodec(NotificationId.apply, _.value)

  given Encoder[NotificationUserId] = uuidEncoder(_.value)
  given Decoder[NotificationUserId] = uuidDecoder(NotificationUserId.apply)
  given Schema[NotificationUserId] =
    uuidSchema(NotificationUserId.apply, _.value)

  given Codec[String, NotificationUserId, CodecFormat.TextPlain] =
    uuidTextCodec(NotificationUserId.apply, _.value)

  given Encoder[NotificationOrderId] = Encoder.encodeOption[UUID].contramap(_.value)
  given Decoder[NotificationOrderId] = Decoder.decodeOption[UUID].map(NotificationOrderId.apply)
  given Schema[NotificationOrderId] =
    Schema.schemaForOption[String]
      .map(value => Some(NotificationOrderId(value.flatMap(uuid => Try(UUID.fromString(uuid)).toOption))))(_.value.map(_.toString))

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
