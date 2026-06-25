package gotrip.http

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.derived

sealed trait HttpError:
  def code: String
  def message: String

object HttpError:

  final case class Unauthorized(
    message: String,
    override val code: String = "UNAUTHORIZED"
  ) extends HttpError

  final case class Forbidden(
    message: String,
    override val code: String = "FORBIDDEN"
  ) extends HttpError

  final case class Validation(
    message: String,
    override val code: String = "VALIDATION_ERROR"
  ) extends HttpError

  final case class NotFound(
    message: String,
    override val code: String = "NOT_FOUND"
  ) extends HttpError

  final case class Conflict(
    message: String,
    override val code: String = "CONFLICT"
  ) extends HttpError

  final case class Internal(
    message: String,
    override val code: String = "INTERNAL_ERROR"
  ) extends HttpError

  given Encoder[Unauthorized] =
    deriveEncoder

  given Decoder[Unauthorized] =
    deriveDecoder

  given Schema[Unauthorized] =
    derived

  given Encoder[Forbidden] =
    deriveEncoder

  given Decoder[Forbidden] =
    deriveDecoder

  given Schema[Forbidden] =
    derived

  given Encoder[Validation] =
    deriveEncoder

  given Decoder[Validation] =
    deriveDecoder

  given Schema[Validation] =
    derived

  given Encoder[NotFound] =
    deriveEncoder

  given Decoder[NotFound] =
    deriveDecoder

  given Schema[NotFound] =
    derived

  given Encoder[Conflict] =
    deriveEncoder

  given Decoder[Conflict] =
    deriveDecoder

  given Schema[Conflict] =
    derived

  given Encoder[Internal] =
    deriveEncoder

  given Decoder[Internal] =
    deriveDecoder

  given Schema[Internal] =
    derived
