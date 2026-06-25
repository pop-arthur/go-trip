package gotrip.http.auth

import gotrip.domain.user.*
import gotrip.domain.userrole.*
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, Schema, Validator}

import java.time.Instant
import scala.util.Try

object AuthCodecs:
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

  given Encoder[RegisterRequest] = Encoder.instance { request =>
    Json.obj(
      "email" -> request.email.asJson,
      "password" -> request.password.asJson,
      "full_name" -> request.fullName.asJson
    )
  }
  given Decoder[RegisterRequest] = Decoder.instance { cursor =>
    for
      email <- cursor.downField("email").as[UserEmail]
      password <- cursor.downField("password").as[String]
      fullName <- cursor.downField("full_name").as[Option[String]].map(UserFullName.apply)
    yield RegisterRequest(email, password, fullName)
  }
  given Schema[RegisterRequest] = derived

  given Encoder[LoginRequest] = deriveEncoder
  given Decoder[LoginRequest] = deriveDecoder
  given Schema[LoginRequest] = derived

  given Encoder[RefreshRequest] = Encoder.instance(request => Json.obj("refresh_token" -> request.refreshToken.asJson))
  given Decoder[RefreshRequest] = Decoder.instance(_.downField("refresh_token").as[String].map(RefreshRequest.apply))
  given Schema[RefreshRequest] = derived

  given Encoder[AuthResponse] = Encoder.instance { response =>
    Json.obj(
      "access_token" -> response.accessToken.asJson,
      "refresh_token" -> response.refreshToken.asJson,
      "token_type" -> response.tokenType.asJson,
      "user" -> response.user.asJson
    )
  }
  given Decoder[AuthResponse] = Decoder.instance { cursor =>
    for
      accessToken <- cursor.downField("access_token").as[String]
      refreshToken <- cursor.downField("refresh_token").as[String]
      tokenType <- cursor.downField("token_type").as[String]
      user <- cursor.downField("user").as[PublicUser]
    yield AuthResponse(accessToken, refreshToken, tokenType, user)
  }
  given Schema[AuthResponse] = derived

  private def parseInstant(value: String): Decoder.Result[Instant] =
    Try(Instant.parse(value)).toEither.left.map(error => io.circe.DecodingFailure(error.getMessage, Nil))
