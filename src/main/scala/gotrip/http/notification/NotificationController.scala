package gotrip.http.notification

import cats.effect.IO
import cats.syntax.flatMap.*
import gotrip.domain.notification.NotificationUserId
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.notification.NotificationService
import gotrip.service.order.OrderStatusSyncService
import sttp.tapir.server.ServerEndpoint
import gotrip.domain.user.value

final class NotificationController(
  service: NotificationService[IO],
  authSupport: AuthSupport,
  orderStatusSyncService: OrderStatusSyncService[IO]
):

  val listCurrentUserNotifications: ServerEndpoint[Any, IO] =
    NotificationEndpoints.listCurrentUserNotifications
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => _ =>
        val userId = NotificationUserId(authUser.userId.value)
        orderStatusSyncService.syncUserOrders(authUser.userId).attempt >> service.listByUser(userId).attempt.map {
          case Right(notifications) => Right(notifications)
          case Left(error)          => Left(HttpError.Internal(error.getMessage))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listCurrentUserNotifications)
