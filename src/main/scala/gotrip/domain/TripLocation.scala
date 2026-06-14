package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

case class TripLocation(
    id: Long,
    tripId: Long,
    locationId: Long,
    visitOrder: Int,
    arrivalDate: Option[Instant],
    departureDate: Option[Instant],
    createdAt: Instant,
    updatedAt: Instant
)

object TripLocation {
  implicit val tripLocationRead: Read[TripLocation] = Read[(Long, Long, Long, Int, Option[Instant], Option[Instant], Instant, Instant)].map {
    case (id, tid, lid, order, arr, dep, createdAt, updatedAt) =>
      TripLocation(id, tid, lid, order, arr, dep, createdAt, updatedAt)
  }
}