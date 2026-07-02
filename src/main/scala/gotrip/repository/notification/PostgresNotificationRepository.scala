package gotrip.repository.notification

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.notification._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{Instant, OffsetDateTime, ZoneOffset}

final class PostgresNotificationRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends NotificationRepository[F]:

  override def create(notification: UserNotification): F[UserNotification] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.insertQuery).flatMap { cmd =>
        cmd.unique(
          (
            notification.id.value,
            notification.userId.value,
            notification.orderId.value,
            NotificationType.toString(notification.notificationType),
            notification.title.value,
            notification.body.value,
            notification.isRead,
            PostgresNotificationRepository.toOffset(notification.sentAt),
            PostgresNotificationRepository.toOffset(notification.createdAt),
            PostgresNotificationRepository.toOffset(notification.updatedAt)
          )
        )
      }
    }

  override def findById(id: NotificationId): F[Option[UserNotification]] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.selectById).flatMap { query =>
        query.option(id.value)
      }
    }

  override def findByUserId(userId: NotificationUserId, limit: Int = 50, offset: Int = 0): F[List[UserNotification]] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.selectByUserId).flatMap { query =>
        query.stream((userId.value, limit, offset), 64).compile.toList
      }
    }

  override def markAsRead(id: NotificationId, updatedAt: Instant): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.markReadCommand).flatMap { cmd =>
        cmd.execute((PostgresNotificationRepository.toOffset(updatedAt), id.value)).map(PostgresNotificationRepository.rowsAffected)
      }
    }

  override def markAllAsRead(userId: NotificationUserId, updatedAt: Instant): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.markAllReadCommand).flatMap { cmd =>
        cmd.execute((PostgresNotificationRepository.toOffset(updatedAt), userId.value)).map(PostgresNotificationRepository.rowsAffected)
      }
    }

  override def delete(id: NotificationId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.deleteCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresNotificationRepository.rowsAffected)
      }
    }

  override def deleteAllForUser(userId: NotificationUserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.deleteAllForUserCommand).flatMap { cmd =>
        cmd.execute(userId.value).map(PostgresNotificationRepository.rowsAffected)
      }
    }

object PostgresNotificationRepository:

  private def rowsAffected(c: Completion): Int = c match {
    case Completion.Update(count) => count
    case Completion.Delete(count) => count
    case _                        => 0
  }

  private val decoder: Decoder[UserNotification] =
    (uuid ~ uuid ~ uuid.opt ~ text ~ text ~ text ~ bool ~ timestamptz ~ timestamptz ~ timestamptz).map {
      case id ~ uid ~ oid ~ ntype ~ title ~ body ~ read ~ sent ~ created ~ updated =>
        UserNotification(
          id = NotificationId(id),
          userId = NotificationUserId(uid),
          orderId = NotificationOrderId(oid),
          notificationType = NotificationType.fromString(ntype).get,
          title = NotificationTitle(title),
          body = NotificationBody(body),
          isRead = read,
          sentAt = sent.toInstant,
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val insertQuery: Query[(UUID, UUID, Option[UUID], String, String, String, Boolean, OffsetDateTime, OffsetDateTime, OffsetDateTime), UserNotification] =
    sql"""
      INSERT INTO user_notifications (id, user_id, order_id, type, title, body, is_read, sent_at, created_at, updated_at)
      VALUES ($uuid, $uuid, ${uuid.opt}, $text, $text, $text, $bool, $timestamptz, $timestamptz, $timestamptz)
      RETURNING id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
    """.query(decoder)

  val selectById: Query[UUID, UserNotification] =
    sql"""
      SELECT id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
      FROM user_notifications WHERE id = $uuid
    """.query(decoder)

  val selectByUserId: Query[(UUID, Int, Int), UserNotification] =
    sql"""
      SELECT id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
      FROM user_notifications
      WHERE user_id = $uuid
      ORDER BY sent_at DESC
      LIMIT $int4 OFFSET $int4
    """.query(decoder)

  val markReadCommand: Command[(OffsetDateTime, UUID)] =
    sql"""
      UPDATE user_notifications
      SET is_read = TRUE, updated_at = $timestamptz
      WHERE id = $uuid
    """.command

  val markAllReadCommand: Command[(OffsetDateTime, UUID)] =
    sql"""
      UPDATE user_notifications
      SET is_read = TRUE, updated_at = $timestamptz
      WHERE user_id = $uuid AND is_read = FALSE
    """.command

  val deleteCommand: Command[UUID] =
    sql"DELETE FROM user_notifications WHERE id = $uuid".command

  val deleteAllForUserCommand: Command[UUID] =
    sql"DELETE FROM user_notifications WHERE user_id = $uuid".command

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): NotificationRepository[F] =
    new PostgresNotificationRepository(sessionPool)
  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)
