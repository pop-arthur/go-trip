package gotrip.service.order

import cats.Monad
import cats.syntax.flatMap.*
import cats.syntax.foldable.*
import cats.syntax.functor.*
import gotrip.domain.order.{Order, OrderStatusEventSource, OrderStatusUpdate}
import gotrip.domain.user.UserId
import gotrip.integration.OrderStatusProvider
import gotrip.repository.order.OrderRepository

final class OrderStatusSyncService[F[_]: Monad](
  orderRepository: OrderRepository[F],
  orderService: OrderService[F],
  orderStatusProvider: OrderStatusProvider[F]
):

  def syncUserOrders(userId: UserId): F[Unit] =
    orderRepository.listExternalByUser(userId).flatMap { orders =>
      orders.traverse_(syncOrder)
    }

  def syncOrder(order: Order): F[Unit] =
    orderStatusProvider.checkUpdates(order).flatMap {
      case Left(_) =>
        Monad[F].pure(())
      case Right(updates) =>
        updates.traverse_ { update =>
          orderService
            .updateStatus(
              userId = order.user_id,
              orderId = order.id,
              update = OrderStatusUpdate(
                status = update.status,
                reason = update.reason,
                payload = update.payload
              ),
              source = OrderStatusEventSource.System
            )
            .void
        }
    }
