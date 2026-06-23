package gotrip.http.user

import gotrip.domain.user._
import gotrip.http.HttpError
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}

object UserCodecs:

  given Encoder[HttpError.Validation] = deriveEncoder
  given Decoder[HttpError.Validation] = deriveDecoder
  given Schema[HttpError.Validation] = derived

  given Encoder[HttpError.NotFound] = deriveEncoder
  given Decoder[HttpError.NotFound] = deriveDecoder
  given Schema[HttpError.NotFound] = derived

  given Encoder[HttpError.Internal] = deriveEncoder
  given Decoder[HttpError.Internal] = deriveDecoder
  given Schema[HttpError.Internal] = derived

  given Encoder[UserId] = Encoder.encodeLong.contramap(_.value)
  given Decoder[UserId] = Decoder.decodeLong.map(UserId.apply)
  given Schema[UserId] =
    Schema.schemaForLong
      .map(value => Some(UserId(value)))(_.value)
      .validate(Validator.inRange(1L, Long.MaxValue).contramap[UserId](_.value))

  given Codec[String, UserId, CodecFormat.TextPlain] =
    Codec.long.map(UserId.apply)(_.value)

  given Encoder[UserEmail] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserEmail] = Decoder.decodeString.map(UserEmail.apply)
  given Schema[UserEmail] =
    Schema.schemaForString.map(value => Some(UserEmail(value)))(_.value)

  given Encoder[UserFullName] = Encoder.encodeOption[String].contramap(_.value)
  given Decoder[UserFullName] = Decoder.decodeOption[String].map(UserFullName.apply)
  given Schema[UserFullName] =
    Schema.schemaForOption[String].map(value => Some(UserFullName(value)))(_.value)

  given Encoder[UserPasswordHash] = Encoder.encodeString.contramap(_.value)
  given Decoder[UserPasswordHash] = Decoder.decodeString.map(UserPasswordHash.apply)
  given Schema[UserPasswordHash] =
    Schema.schemaForString.map(value => Some(UserPasswordHash(value)))(_.value)

  given Encoder[User] = deriveEncoder
  given Decoder[User] = deriveDecoder
  given Schema[User] = derived

  case class UserUpdate(email: Option[UserEmail], fullName: Option[UserFullName])
  given Encoder[UserUpdate] = deriveEncoder
  given Decoder[UserUpdate] = deriveDecoder
  given Schema[UserUpdate] = derived