package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait ProviderType
object ProviderType {
  case object AIRLINE extends ProviderType
  case object HOTEL extends ProviderType
  case object TOUR_COMPANY extends ProviderType
  case object TRANSPORT_COMPANY extends ProviderType
  case object BOOKING_PLATFORM extends ProviderType
  case object INSURANCE_COMPANY extends ProviderType
  case object OTHER extends ProviderType

  implicit val providerTypeMeta: Meta[ProviderType] =
    Meta[String].imap {
      case "AIRLINE"            => AIRLINE
      case "HOTEL"              => HOTEL
      case "TOUR_COMPANY"       => TOUR_COMPANY
      case "TRANSPORT_COMPANY"  => TRANSPORT_COMPANY
      case "BOOKING_PLATFORM"   => BOOKING_PLATFORM
      case "INSURANCE_COMPANY"  => INSURANCE_COMPANY
      case "OTHER"              => OTHER
    }(_.toString)
}

case class Provider(
    id: Long,
    name: String,
    providerType: ProviderType,
    website: Option[String],
    supportContact: Option[String],
    createdAt: Instant,
    updatedAt: Instant
)

object Provider {
  implicit val providerRead: Read[Provider] = Read[(Long, String, ProviderType, Option[String], Option[String], Instant, Instant)].map {
    case (id, name, pType, website, contact, createdAt, updatedAt) =>
      Provider(id, name, pType, website, contact, createdAt, updatedAt)
  }
}