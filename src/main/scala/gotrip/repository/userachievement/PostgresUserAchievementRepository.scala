package gotrip.repository.userachievement

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.achievement._
import gotrip.domain.userachievement._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{OffsetDateTime, ZoneOffset}

final class PostgresUserAchievementRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends UserAchievementRepository[F]:

  override def create(userId: UserId, achievementId: AchievementId): F[UserAchievement] =
    sessionPool.use { session =>
      session.prepare(PostgresUserAchievementRepository.insertQuery).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.unique((userId.value, achievementId.value, now, now, now))
      }
    }

  override def findByUserId(userId: UserId): F[List[UserAchievement]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserAchievementRepository.selectByUserId).flatMap { query =>
        query.stream(userId.value, 64).compile.toList
      }
    }

  override def findByAchievementId(achievementId: AchievementId): F[List[UserAchievement]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserAchievementRepository.selectByAchievementId).flatMap { query =>
        query.stream(achievementId.value, 64).compile.toList
      }
    }

  override def delete(userId: UserId, achievementId: AchievementId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserAchievementRepository.deleteCommand).flatMap { cmd =>
        cmd.execute((userId.value, achievementId.value)).map(PostgresUserAchievementRepository.rowsAffected)
      }
    }

object PostgresUserAchievementRepository {

  private def rowsAffected(c: Completion): Int = c match {
    case Completion.Delete(count) => count
    case _                        => 0
  }

  private val decoder: Decoder[UserAchievement] =
    (int8 ~ int8 ~ int8 ~ timestamptz ~ timestamptz ~ timestamptz).map {
      case id ~ uid ~ aid ~ unlocked ~ created ~ updated =>
        UserAchievement(
          id = UserAchievementId(id),
          userId = UserId(uid),
          achievementId = AchievementId(aid),
          unlockedAt = unlocked.toInstant,
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val insertQuery: Query[(Long, Long, OffsetDateTime, OffsetDateTime, OffsetDateTime), UserAchievement] =
    sql"""
      INSERT INTO user_achievements (user_id, achievement_id, unlocked_at, created_at, updated_at)
      VALUES ($int8, $int8, $timestamptz, $timestamptz, $timestamptz)
      ON CONFLICT (user_id, achievement_id) DO NOTHING
      RETURNING id, user_id, achievement_id, unlocked_at, created_at, updated_at
    """.query(decoder)

  val selectByUserId: Query[Long, UserAchievement] =
    sql"""
      SELECT id, user_id, achievement_id, unlocked_at, created_at, updated_at
      FROM user_achievements
      WHERE user_id = $int8
      ORDER BY unlocked_at
    """.query(decoder)

  val selectByAchievementId: Query[Long, UserAchievement] =
    sql"""
      SELECT id, user_id, achievement_id, unlocked_at, created_at, updated_at
      FROM user_achievements
      WHERE achievement_id = $int8
      ORDER BY unlocked_at
    """.query(decoder)

  val deleteCommand: Command[(Long, Long)] =
    sql"DELETE FROM user_achievements WHERE user_id = $int8 AND achievement_id = $int8".command

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): UserAchievementRepository[F] =
    new PostgresUserAchievementRepository(sessionPool)
}