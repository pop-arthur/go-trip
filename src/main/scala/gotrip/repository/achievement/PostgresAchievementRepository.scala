package gotrip.repository.achievement

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.achievement._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{Instant, OffsetDateTime, ZoneOffset}

final class PostgresAchievementRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends AchievementRepository[F]:

  override def findAll(): F[List[Achievement]] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.selectAll).flatMap { query =>
        query.stream(Void, 64).compile.toList
      }
    }

  override def findById(id: AchievementId): F[Option[Achievement]] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.selectById).flatMap { query =>
        query.option(id.value)
      }
    }

  override def findByCode(code: AchievementCode): F[Option[Achievement]] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.selectByCode).flatMap { query =>
        query.option(code.value)
      }
    }

  override def create(achievement: Achievement): F[Achievement] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.insertQuery).flatMap { cmd =>
        cmd.unique(
          (
            achievement.id.value,
            achievement.code.value,
            achievement.title.value,
            achievement.description.value,
            AchievementConditionType.toString(achievement.conditionType),
            achievement.conditionValue,
            achievement.iconUrl.value,
            PostgresAchievementRepository.toOffset(achievement.createdAt),
            PostgresAchievementRepository.toOffset(achievement.updatedAt)
          )
        )
      }
    }

  override def update(achievement: Achievement): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.updateCommand).flatMap { cmd =>
        cmd.execute(
          (
            achievement.code.value,
            achievement.title.value,
            achievement.description.value,
            AchievementConditionType.toString(achievement.conditionType),
            achievement.conditionValue,
            achievement.iconUrl.value,
            PostgresAchievementRepository.toOffset(achievement.updatedAt),
            achievement.id.value
          )
        ).map(PostgresAchievementRepository.rowsAffected)
      }
    }

  override def delete(id: AchievementId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresAchievementRepository.deleteCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresAchievementRepository.rowsAffected)
      }
    }

object PostgresAchievementRepository:

  private def rowsAffected(c: Completion): Int = c match {
    case Completion.Insert(count) => count
    case Completion.Update(count) => count
    case Completion.Delete(count) => count
    case _                        => 0
  }

  private val decoder: Decoder[Achievement] =
    (uuid ~ text ~ text ~ text.opt ~ text ~ int4 ~ text.opt ~ timestamptz ~ timestamptz).map {
      case id ~ code ~ title ~ desc ~ condType ~ condVal ~ icon ~ created ~ updated =>
        Achievement(
          id = AchievementId(id),
          code = AchievementCode(code),
          title = AchievementTitle(title),
          description = AchievementDescription(desc),
          conditionType = AchievementConditionType.fromString(condType).get,
          conditionValue = condVal,
          iconUrl = AchievementIconUrl(icon),
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val selectAll: Query[Void, Achievement] =
    sql"""
      SELECT id, code::text, title::text, description::text, condition_type::text, condition_value, icon_url::text, created_at, updated_at
      FROM achievements
      ORDER BY id
    """.query(decoder)

  val selectById: Query[UUID, Achievement] =
    sql"""
      SELECT id, code::text, title::text, description::text, condition_type::text, condition_value, icon_url::text, created_at, updated_at
      FROM achievements WHERE id = $uuid
    """.query(decoder)

  val selectByCode: Query[String, Achievement] =
    sql"""
      SELECT id, code::text, title::text, description::text, condition_type::text, condition_value, icon_url::text, created_at, updated_at
      FROM achievements WHERE code = $text
    """.query(decoder)

  val insertQuery: Query[(UUID, String, String, Option[String], String, Int, Option[String], OffsetDateTime, OffsetDateTime), Achievement] =
    sql"""
      INSERT INTO achievements (id, code, title, description, condition_type, condition_value, icon_url, created_at, updated_at)
      VALUES ($uuid, $text, $text, ${text.opt}, $text, $int4, ${text.opt}, $timestamptz, $timestamptz)
      RETURNING id, code::text, title::text, description::text, condition_type::text, condition_value, icon_url::text, created_at, updated_at
    """.query(decoder)

  val updateCommand: Command[(String, String, Option[String], String, Int, Option[String], OffsetDateTime, UUID)] =
    sql"""
      UPDATE achievements
      SET code = $text, title = $text, description = ${text.opt}, condition_type = $text,
          condition_value = $int4, icon_url = ${text.opt}, updated_at = $timestamptz
      WHERE id = $uuid
    """.command

  val deleteCommand: Command[UUID] =
    sql"DELETE FROM achievements WHERE id = $uuid".command

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): AchievementRepository[F] =
    new PostgresAchievementRepository(sessionPool)
  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)
