package gotrip.http.orderfile

import gotrip.domain.order.*
import gotrip.http.ApiError
import gotrip.http.UuidCodecs.*
import gotrip.http.order.OrderCodecs
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.derived
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema}

object OrderFileCodecs:
  import OrderCodecs.given

  given Encoder[ApiError] = deriveEncoder
  given Decoder[ApiError] = deriveDecoder
  given Schema[ApiError] = derived

  given Encoder[OrderFileId] =
    uuidEncoder(_.value)

  given Decoder[OrderFileId] =
    uuidDecoder(OrderFileId.apply)

  given Schema[OrderFileId] =
    uuidSchema(OrderFileId.apply, _.value)

  given Codec[String, OrderFileId, CodecFormat.TextPlain] =
    uuidTextCodec(OrderFileId.apply, _.value)

  given Encoder[OrderFileUrl] =
    Encoder.encodeString.contramap(_.value)

  given Decoder[OrderFileUrl] =
    Decoder.decodeString.map(OrderFileUrl.apply)

  given Schema[OrderFileUrl] =
    Schema.schemaForString.map(value => Some(OrderFileUrl(value)))(_.value)

  given Encoder[FileType] =
    Encoder.encodeString.contramap(encodeFileType)

  given Decoder[FileType] =
    Decoder.decodeString.emap(value => parseFileType(value).left.map(_.message))

  given Schema[FileType] =
    Schema.schemaForString.map(value => parseFileType(value).toOption)(encodeFileType)

  given Codec[String, FileType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { value =>
      parseFileType(value) match
        case Right(fileType) => DecodeResult.Value(fileType)
        case Left(error)     => DecodeResult.Error(value, new Exception(error.message))
    }(encodeFileType)

  given Encoder[OrderFile] = deriveEncoder
  given Decoder[OrderFile] = deriveDecoder
  given Schema[OrderFile] = derived

  given Encoder[OrderFileCreate] = deriveEncoder
  given Decoder[OrderFileCreate] = deriveDecoder
  given Schema[OrderFileCreate] = derived

  private def parseFileType(value: String): Either[ApiError, FileType] =
    value.toUpperCase match
      case "PDF"   => Right(FileType.Pdf)
      case "IMAGE" => Right(FileType.Image)
      case "EMAIL" => Right(FileType.Email)
      case "JSON"  => Right(FileType.Json)
      case "OTHER" => Right(FileType.Other)
      case other   => Left(ApiError("VALIDATION_ERROR", s"Unknown file type: $other"))

  private def encodeFileType(fileType: FileType): String =
    fileType match
      case FileType.Pdf   => "PDF"
      case FileType.Image => "IMAGE"
      case FileType.Email => "EMAIL"
      case FileType.Json  => "JSON"
      case FileType.Other => "OTHER"
