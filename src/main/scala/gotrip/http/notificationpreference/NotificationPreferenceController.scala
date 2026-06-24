package gotrip.http.notificationpreference

import cats.effect.IO
import gotrip.domain.user.UserId
import gotrip.http.HttpError
import gotrip.service.notificationpreference.NotificationPreferenceService
import sttp.tapir.server.ServerEndpoint
import NotificationPreferenceCodecs.NotificationPreferenceUpdate

final class NotificationPreferenceController(service: NotificationPreferenceService[IO]):

  val getPreference: ServerEndpoint[Any, IO] =
    NotificationPreferenceEndpoints.getPreference.serverLogic { _ =>
      val userId = UserId(1L)
      service.getByUserId(userId).attempt.map {
        case Right(Some(pref)) => Right(pref)
        case Right(None)       => Left(HttpError.NotFound("Notification preference not found"))
        case Left(error)       => Left(HttpError.Internal(error.getMessage))
      }
    }

  val updatePreference: ServerEndpoint[Any, IO] =
    NotificationPreferenceEndpoints.updatePreference.serverLogic { update =>
      val userId = UserId(1L)
      service.setStatus(userId, update.isEnabled).attempt.map {
        case Right(pref) => Right(pref)
        case Left(error) => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getPreference, updatePreference)