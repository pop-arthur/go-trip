package gotrip.http.notificationpreference

import cats.effect.IO
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.notificationpreference.NotificationPreferenceService
import sttp.tapir.server.ServerEndpoint
import NotificationPreferenceCodecs.NotificationPreferenceUpdate

final class NotificationPreferenceController(service: NotificationPreferenceService[IO], authSupport: AuthSupport):

  val getPreference: ServerEndpoint[Any, IO] =
    NotificationPreferenceEndpoints.getPreference
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => _ =>
        service.getByUserId(authUser.userId).attempt.map {
          case Right(Some(pref)) => Right(pref)
          case Right(None)       => Left(HttpError.NotFound("Notification preference not found"))
          case Left(error)       => Left(HttpError.Internal(error.getMessage))
        }
      }

  val updatePreference: ServerEndpoint[Any, IO] =
    NotificationPreferenceEndpoints.updatePreference
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => update =>
        service.setStatus(authUser.userId, update.isEnabled).attempt.map {
          case Right(pref) => Right(pref)
          case Left(error) => Left(HttpError.Internal(error.getMessage))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getPreference, updatePreference)
