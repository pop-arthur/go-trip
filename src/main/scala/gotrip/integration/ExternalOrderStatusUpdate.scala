package gotrip.integration

import cats.Functor
import cats.effect.Clock
import cats.syntax.functor.*
import gotrip.domain.order.{OrderId, OrderStatus}
import gotrip.service.GeneratedData
import io.circe.Json

import java.time.Instant

final case class ExternalOrderStatusUpdate(
  orderId: OrderId,
  externalOrderId: String,
  status: OrderStatus,
  occurredAt: Instant,
  reason: Option[String] = None,
  payload: Option[Json] = None
)

object ExternalOrderStatusUpdate:
  def create[F[_]: Clock: Functor](
    orderId: OrderId,
    externalOrderId: String,
    status: OrderStatus,
    reason: Option[String] = None,
    payload: Option[Json] = None
  ): F[ExternalOrderStatusUpdate] =
    GeneratedData.now[F].map { now =>
      ExternalOrderStatusUpdate(
        orderId = orderId,
        externalOrderId = externalOrderId,
        status = status,
        occurredAt = now,
        reason = reason,
        payload = payload
      )
    }
