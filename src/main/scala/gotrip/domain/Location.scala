package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait LocationType
object LocationType {
  case object COUNTRY extends LocationType
  case object CITY extends LocationType
  case object AIRPORT extends LocationType
  case object TRAIN_STATION extends LocationType
  case object BUS_STATION extends LocationType
  case object PORT extends LocationType
  case object HOTEL extends LocationType
  case object MEETING_POINT extends LocationType
  case object ATTRACTION extends LocationType
  case object OTHER extends LocationType

  implicit val locationTypeMeta: Meta[LocationType] =
    Meta[String].imap {
      case "COUNTRY"        => COUNTRY
      case "CITY"           => CITY
      case "AIRPORT"        => AIRPORT
      case "TRAIN_STATION"  => TRAIN_STATION
      case "BUS_STATION"    => BUS_STATION
      case "PORT"           => PORT
      case "HOTEL"          => HOTEL
      case "MEETING_POINT"  => MEETING_POINT
      case "ATTRACTION"     => ATTRACTION
      case "OTHER"          => OTHER
    }(_.toString)
}

case class Location(
    id: Long,
    name: String,
    locationType: LocationType,
    country: Option[String],
    city: Option[String],
    address: Option[String],
    latitude: Option[Double],
    longitude: Option[Double],
    createdAt: Instant,
    updatedAt: Instant
)

object Location {
  implicit val locationRead: Read[Location] = Read[(Long, String, LocationType, Option[String], Option[String], Option[String], Option[Double], Option[Double], Instant, Instant)].map {
    case (id, name, locType, country, city, address, lat, lon, createdAt, updatedAt) =>
      Location(id, name, locType, country, city, address, lat, lon, createdAt, updatedAt)
  }
}