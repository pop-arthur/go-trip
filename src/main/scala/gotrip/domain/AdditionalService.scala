package gotrip.domain

import java.math.BigDecimal
import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta
import gotrip.domain.ServiceType

case class AdditionalService(
    id: Long,
    title: String,
    description: Option[String],
    serviceType: ServiceType,
    providerId: Option[Long],
    locationId: Option[Long],
    priceAmount: Option[BigDecimal],
    priceCurrency: Option[String],
    isActive: Boolean,
    createdAt: Instant,
    updatedAt: Instant
)

object AdditionalService {
  implicit val addServiceRead: Read[AdditionalService] = Read[(Long, String, Option[String], ServiceType, Option[Long], Option[Long],
    Option[BigDecimal], Option[String], Boolean, Instant, Instant)].map {
    case (id, title, desc, st, pid, lid, price, cur, active, createdAt, updatedAt) =>
      AdditionalService(id, title, desc, st, pid, lid, price, cur, active, createdAt, updatedAt)
  }
}