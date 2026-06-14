package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait ReviewTargetType
object ReviewTargetType {
  case object PROVIDER extends ReviewTargetType
  case object LOCATION extends ReviewTargetType
  case object ORDER extends ReviewTargetType
  case object ADDITIONAL_SERVICE extends ReviewTargetType

  implicit val reviewTargetTypeMeta: Meta[ReviewTargetType] =
    Meta[String].imap {
      case "PROVIDER"           => PROVIDER
      case "LOCATION"           => LOCATION
      case "ORDER"              => ORDER
      case "ADDITIONAL_SERVICE" => ADDITIONAL_SERVICE
    }(_.toString)
}

case class Review(
    id: Long,
    userId: Long,
    targetType: ReviewTargetType,
    targetId: Long,
    rating: Int,
    text: Option[String],
    createdAt: Instant,
    updatedAt: Instant
)

object Review {
  implicit val reviewRead: Read[Review] = Read[(Long, Long, ReviewTargetType, Long, Int, Option[String], Instant, Instant)].map {
    case (id, uid, ttype, tid, rating, text, createdAt, updatedAt) =>
      Review(id, uid, ttype, tid, rating, text, createdAt, updatedAt)
  }
}