package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait FileType
object FileType {
  case object PDF extends FileType
  case object IMAGE extends FileType
  case object EMAIL extends FileType
  case object JSON extends FileType
  case object OTHER extends FileType

  implicit val fileTypeMeta: Meta[FileType] =
    Meta[String].imap {
      case "PDF"   => PDF
      case "IMAGE" => IMAGE
      case "EMAIL" => EMAIL
      case "JSON"  => JSON
      case "OTHER" => OTHER
    }(_.toString)
}

case class OrderFile(
    id: Long,
    orderId: Long,
    fileUrl: String,
    fileType: FileType,
    parsedData: Option[String],
    uploadedAt: Instant
)

object OrderFile {
  implicit val orderFileRead: Read[OrderFile] = Read[(Long, Long, String, FileType, Option[String], Instant)].map {
    case (id, oid, url, ftype, data, uploadedAt) =>
      OrderFile(id, oid, url, ftype, data, uploadedAt)
  }
}