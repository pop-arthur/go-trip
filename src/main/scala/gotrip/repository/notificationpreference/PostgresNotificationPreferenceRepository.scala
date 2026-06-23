package gotrip.repository.notificationpreference

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.notificationpreference._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{OffsetDateTime, ZoneOffset}

final class PostgresNotificationPreferenceRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends NotificationPreferenceRepository[F]:

  override def getByUserId(userId: UserId): F[Option[NotificationPreference]] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationPreferenceRepository.selectByUserId).flatMap { query =>
        query.option(userId.value)
      }
    }

  override def upsert(userId: UserId, isEnabled: Boolean): F[NotificationPreference] =
    sessionPool.use { session =>
      session.prepare(PostgresNotificationPreferenceRepository.upsertQuery).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.unique((userId.value, isEnabled, now, now))
      }
    }

object PostgresNotificationPreferenceRepository {

  private val decoder: Decoder[NotificationPreference] =
    (int8 ~ int8 ~ bool ~ timestamptz ~ timestamptz).map {
      case id ~ uid ~ enabled ~ created ~ updated =>
        NotificationPreference(
          id = NotificationPreferenceId(id),
          userId = UserId(uid),
          isEnabled = enabled,
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val selectByUserId: Query[Long, NotificationPreference] =
    sql"""
      SELECT id, user_id, is_enabled, created_at, updated_at
      FROM notification_preferences
      WHERE user_id = $int8
    """.query(decoder)

  val upsertQuery: Query[(Long, Boolean, OffsetDateTime, OffsetDateTime), NotificationPreference] =
    sql"""
      INSERT INTO notification_preferences (user_id, is_enabled, created_at, updated_at)
      VALUES ($int8, $bool, $timestamptz, $timestamptz)
      ON CONFLICT (user_id) DO UPDATE
      SET is_enabled = EXCLUDED.is_enabled, updated_at = EXCLUDED.updated_at
      RETURNING id, user_id, is_enabled, created_at, updated_at
    """.query(decoder)

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): NotificationPreferenceRepository[F] =
    new PostgresNotificationPreferenceRepository(sessionPool)
}