package gotrip.domain

import java.time.Instant
import java.math.BigDecimal
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait ServiceType
object ServiceType {
  case object FLIGHT extends ServiceType
  case object TRAIN extends ServiceType
  case object BUS extends ServiceType
  case object HOTEL extends ServiceType
  case object TOUR extends ServiceType
  case object CAR_RENTAL extends ServiceType
  case object INSURANCE extends ServiceType
  case object TAXI extends ServiceType
  case object ESIM extends ServiceType
  case object LOUNGE extends ServiceType
  case object EXTRA_BAGGAGE extends ServiceType
  case object OTHER extends ServiceType

  implicit val serviceTypeMeta: Meta[ServiceType] =
    Meta[String].imap {
      case "FLIGHT"        => FLIGHT
      case "TRAIN"         => TRAIN
      case "BUS"           => BUS
      case "HOTEL"         => HOTEL
      case "TOUR"          => TOUR
      case "CAR_RENTAL"    => CAR_RENTAL
      case "INSURANCE"     => INSURANCE
      case "TAXI"          => TAXI
      case "ESIM"          => ESIM
      case "LOUNGE"        => LOUNGE
      case "EXTRA_BAGGAGE" => EXTRA_BAGGAGE
      case "OTHER"         => OTHER
    }(_.toString)
}

sealed trait OrderStatus
object OrderStatus {
  case object PENDING_VERIFICATION extends OrderStatus
  case object CONFIRMED extends OrderStatus
  case object DELAYED extends OrderStatus
  case object CANCELLED extends OrderStatus
  case object COMPLETED extends OrderStatus
  case object REFUND_PENDING extends OrderStatus
  case object REFUNDED extends OrderStatus

  implicit val orderStatusMeta: Meta[OrderStatus] =
    Meta[String].imap {
      case "PENDING_VERIFICATION" => PENDING_VERIFICATION
      case "CONFIRMED"            => CONFIRMED
      case "DELAYED"              => DELAYED
      case "CANCELLED"            => CANCELLED
      case "COMPLETED"            => COMPLETED
      case "REFUND_PENDING"       => REFUND_PENDING
      case "REFUNDED"             => REFUNDED
    }(_.toString)
}

case class Order(
    id: Long,
    userId: Long,
    tripId: Long,
    providerId: Option[Long],
    serviceType: ServiceType,
    externalOrderId: Option[String],
    title: String,
    status: OrderStatus,
    priceAmount: Option[BigDecimal],
    priceCurrency: Option[String],
    startDatetime: Option[Instant],
    endDatetime: Option[Instant],
    departureLocationId: Option[Long],
    arrivalLocationId: Option[Long],
    createdAt: Instant,
    updatedAt: Instant
)

object Order {
  implicit val orderRead: Read[Order] = Read[(Long, Long, Long, Option[Long], ServiceType, Option[String], String, OrderStatus,
    Option[BigDecimal], Option[String], Option[Instant], Option[Instant], Option[Long], Option[Long], Instant, Instant)].map {
    case (id, uid, tid, pid, st, extId, title, status, price, cur, start, end, depLoc, arrLoc, createdAt, updatedAt) =>
      Order(id, uid, tid, pid, st, extId, title, status, price, cur, start, end, depLoc, arrLoc, createdAt, updatedAt)
  }
}