package gotrip.http.user

import gotrip.domain.user._
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.UuidCodecs.*
import gotrip.http.auth.PublicUser
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema}

import java.time.Instant
import scala.util.Try

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

  given Encoder[UserId] = uuidEncoder(_.value)
  given Decoder[UserId] = uuidDecoder(UserId.apply)
  given Schema[UserId] = uuidSchema(UserId.apply, _.value)

  given Codec[String, UserId, CodecFormat.TextPlain] =
    uuidTextCodec(UserId.apply, _.value)

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

  given Encoder[Role] = Encoder.encodeString.contramap(Role.toString)
  given Decoder[Role] = Decoder.decodeString.emap(value => Role.fromString(value).toRight(s"Invalid role: $value"))
  given Schema[Role] = Schema.schemaForString.map(Role.fromString)(Role.toString)

  given Encoder[PublicUser] = Encoder.instance { user =>
    Json.obj(
      "id" -> user.id.asJson,
      "email" -> user.email.asJson,
      "full_name" -> user.fullName.asJson,
      "roles" -> user.roles.asJson,
      "created_at" -> user.createdAt.toString.asJson,
      "updated_at" -> user.updatedAt.toString.asJson
    )
  }
  given Decoder[PublicUser] = Decoder.instance { cursor =>
    for
      id <- cursor.downField("id").as[UserId]
      email <- cursor.downField("email").as[UserEmail]
      fullName <- cursor.downField("full_name").as[UserFullName]
      roles <- cursor.downField("roles").as[List[Role]]
      createdAt <- cursor.downField("created_at").as[String].flatMap(parseInstant)
      updatedAt <- cursor.downField("updated_at").as[String].flatMap(parseInstant)
    yield PublicUser(id, email, fullName, roles, createdAt, updatedAt)
  }
  given Schema[PublicUser] = derived

  case class UserUpdate(email: Option[UserEmail], fullName: Option[UserFullName])
  given Encoder[UserUpdate] = Encoder.instance { update =>
    Json.obj(
      "email" -> update.email.asJson,
      "full_name" -> update.fullName.asJson
    )
  }
  given Decoder[UserUpdate] = Decoder.instance { cursor =>
    for
      email <- cursor.downField("email").as[Option[UserEmail]]
      fullName <- cursor.downField("full_name").as[Option[Option[String]]].map(_.map(UserFullName.apply))
    yield UserUpdate(email, fullName)
  }
  given Schema[UserUpdate] = derived

  private def parseInstant(value: String): Decoder.Result[Instant] =
    Try(Instant.parse(value)).toEither.left.map(error => io.circe.DecodingFailure(error.getMessage, Nil))
