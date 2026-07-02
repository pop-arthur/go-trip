package gotrip.http

import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

import java.util.UUID
import scala.util.Try

object UuidCodecs:
  def uuidEncoder[A](unwrap: A => UUID): Encoder[A] =
    Encoder.encodeString.contramap(value => unwrap(value).toString)

  def uuidDecoder[A](wrap: UUID => A): Decoder[A] =
    Decoder.decodeString.emap(value => parseUuid(value).map(wrap))

  def uuidSchema[A](wrap: UUID => A, unwrap: A => UUID): Schema[A] =
    Schema.schemaForString
      .map(value => parseUuid(value).toOption.map(wrap))(value => unwrap(value).toString)

  def uuidTextCodec[A](wrap: UUID => A, unwrap: A => UUID): Codec[String, A, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseUuid(value) match
        case Right(uuid) => DecodeResult.Value(wrap(uuid))
        case Left(error) => DecodeResult.Error(value, new IllegalArgumentException(error))
    }(value => unwrap(value).toString)

  private def parseUuid(value: String): Either[String, UUID] =
    Try(UUID.fromString(value)).toEither.left.map(_.getMessage)
