package gotrip.http.notification

import cats.effect.IO
import gotrip.domain.notification.NotificationUserId
import gotrip.http.HttpError
import gotrip.service.notification.NotificationService
import sttp.tapir.server.ServerEndpoint

final class NotificationController(service: NotificationService[IO]):

  val listCurrentUserNotifications: ServerEndpoint[Any, IO] =
    NotificationEndpoints.listCurrentUserNotifications.serverLogic { _ =>
      val userId = NotificationUserId(1L)
      service.listByUser(userId).attempt.map {
        case Right(notifications) => Right(notifications)
        case Left(error)          => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listCurrentUserNotifications)
