package gotrip.repository.notification

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.notification._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{OffsetDateTime, ZoneOffset}

final class PostgresNotificationRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends NotificationRepository[F]:

  override def create(notification: UserNotification): F[UserNotification] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.insertQuery).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.unique(
          (
            notification.userId.value,
            notification.orderId.value,
            NotificationType.toString(notification.notificationType),
            notification.title.value,
            notification.body.value,
            notification.isRead,
            now,
            now,
            now
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

  override def markAsRead(id: NotificationId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.markReadCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresNotificationRepository.rowsAffected)
      }
    }

  override def markAllAsRead(userId: NotificationUserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationRepository.markAllReadCommand).flatMap { cmd =>
        cmd.execute(userId.value).map(PostgresNotificationRepository.rowsAffected)
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
    (int8 ~ int8 ~ int8.opt ~ text ~ text ~ text ~ bool ~ timestamptz ~ timestamptz ~ timestamptz).map {
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

  val insertQuery: Query[(Long, Option[Long], String, String, String, Boolean, OffsetDateTime, OffsetDateTime, OffsetDateTime), UserNotification] =
    sql"""
      INSERT INTO user_notifications (user_id, order_id, type, title, body, is_read, sent_at, created_at, updated_at)
      VALUES ($int8, ${int8.opt}, $text, $text, $text, $bool, $timestamptz, $timestamptz, $timestamptz)
      RETURNING id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
    """.query(decoder)

  val selectById: Query[Long, UserNotification] =
    sql"""
      SELECT id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
      FROM user_notifications WHERE id = $int8
    """.query(decoder)

  val selectByUserId: Query[(Long, Int, Int), UserNotification] =
    sql"""
      SELECT id, user_id, order_id, type::text, title::text, body::text, is_read, sent_at, created_at, updated_at
      FROM user_notifications
      WHERE user_id = $int8
      ORDER BY sent_at DESC
      LIMIT $int4 OFFSET $int4
    """.query(decoder)

  val markReadCommand: Command[Long] =
    sql"""
      UPDATE user_notifications
      SET is_read = TRUE, updated_at = NOW()
      WHERE id = $int8
    """.command

  val markAllReadCommand: Command[Long] =
    sql"""
      UPDATE user_notifications
      SET is_read = TRUE, updated_at = NOW()
      WHERE user_id = $int8 AND is_read = FALSE
    """.command

  val deleteCommand: Command[Long] =
    sql"DELETE FROM user_notifications WHERE id = $int8".command

  val deleteAllForUserCommand: Command[Long] =
    sql"DELETE FROM user_notifications WHERE user_id = $int8".command

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): NotificationRepository[F] =
    new PostgresNotificationRepository(sessionPool)