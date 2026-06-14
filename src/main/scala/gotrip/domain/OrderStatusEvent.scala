package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait EventSource
object EventSource {
  case object SYSTEM extends EventSource
  case object ADMIN_SIMULATION extends EventSource
  case object USER_EDIT extends EventSource

  implicit val eventSourceMeta: Meta[EventSource] =
    Meta[String].imap {
      case "system"           => SYSTEM
      case "admin_simulation" => ADMIN_SIMULATION
      case "user_edit"        => USER_EDIT
    }(_.toString.toLowerCase)
}

case class OrderStatusEvent(
    id: Long,
    orderId: Long,
    oldStatus: Option[OrderStatus],
    newStatus: OrderStatus,
    reason: Option[String],
    payload: Option[String],
    source: EventSource,
    createdAt: Instant
)

object OrderStatusEvent {
  implicit val eventRead: Read[OrderStatusEvent] = Read[(Long, Long, Option[OrderStatus], OrderStatus, Option[String], Option[String], EventSource, Instant)].map {
    case (id, oid, old, newSt, reason, payload, source, createdAt) =>
      OrderStatusEvent(id, oid, old, newSt, reason, payload, source, createdAt)
  }
}