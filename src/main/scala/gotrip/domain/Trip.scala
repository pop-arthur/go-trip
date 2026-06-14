package gotrip.domain

import java.time.Instant
import java.time.LocalDate
import doobie._
import doobie.postgres.implicits.JavaLocalDateMeta
import doobie.postgres.implicits.JavaInstantMeta

sealed trait TripStatus
object TripStatus {
  case object PLANNED extends TripStatus
  case object ACTIVE extends TripStatus
  case object COMPLETED extends TripStatus
  case object CANCELLED extends TripStatus

  implicit val tripStatusMeta: Meta[TripStatus] =
    Meta[String].imap {
      case "PLANNED"   => PLANNED
      case "ACTIVE"    => ACTIVE
      case "COMPLETED" => COMPLETED
      case "CANCELLED" => CANCELLED
    }(_.toString)
}

case class Trip(
    id: Long,
    userId: Long,
    title: String,
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    status: TripStatus,
    createdAt: Instant,
    updatedAt: Instant
)

object Trip {
  implicit val tripRead: Read[Trip] = Read[(Long, Long, String, Option[LocalDate], Option[LocalDate], TripStatus, Instant, Instant)].map {
    case (id, uid, title, start, end, status, createdAt, updatedAt) =>
      Trip(id, uid, title, start, end, status, createdAt, updatedAt)
  }
}