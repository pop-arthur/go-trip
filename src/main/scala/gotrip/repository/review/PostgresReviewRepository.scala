package gotrip.repository.review

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.review._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{OffsetDateTime, ZoneOffset}

final class PostgresReviewRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends ReviewRepository[F]:

  override def create(review: Review): F[Review] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.insertQuery).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.unique(
          (
            review.userId.value,
            ReviewTargetType.toString(review.targetType),
            review.targetId.value,
            review.rating.value,
            review.text.value,
            now,
            now
          )
        )
      }
    }

  override def findById(id: ReviewId): F[Option[Review]] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.selectById).flatMap { query =>
        query.option(id.value)
      }
    }

  override def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.selectByTarget).flatMap { query =>
        query.stream((ReviewTargetType.toString(targetType), targetId.value), 64).compile.toList
      }
    }

  override def findByUserId(userId: UserId): F[List[Review]] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.selectByUserId).flatMap { query =>
        query.stream(userId.value, 64).compile.toList
      }
    }

  override def update(review: Review): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.updateCommand).flatMap { cmd =>
        cmd.execute(
          (
            review.rating.value,
            review.text.value,
            review.id.value
          )
        ).map(PostgresReviewRepository.rowsAffected)
      }
    }

  override def delete(id: ReviewId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.deleteCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresReviewRepository.rowsAffected)
      }
    }

  override def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] =
    sessionPool.use { session =>
      session.prepare(PostgresReviewRepository.averageQuery).flatMap { query =>
        query.option((ReviewTargetType.toString(targetType), targetId.value))
      }
    }

object PostgresReviewRepository {

  private def rowsAffected(c: Completion): Int = c match {
    case Completion.Insert(count) => count
    case Completion.Update(count) => count
    case Completion.Delete(count) => count
    case _                        => 0
  }

  private val decoder: Decoder[Review] =
    (int8 ~ int8 ~ text ~ int8 ~ int4 ~ text.opt ~ timestamptz ~ timestamptz).map {
      case id ~ uid ~ ttype ~ tid ~ rating ~ text ~ created ~ updated =>
        Review(
          id = ReviewId(id),
          userId = UserId(uid),
          targetType = ReviewTargetType.fromString(ttype).get,
          targetId = ReviewTargetId(tid),
          rating = ReviewRating(rating),
          text = ReviewText(text),
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val insertQuery: Query[(Long, String, Long, Int, Option[String], OffsetDateTime, OffsetDateTime), Review] =
    sql"""
      INSERT INTO reviews (user_id, target_type, target_id, rating, text, created_at, updated_at)
      VALUES ($int8, $text, $int8, $int4, ${text.opt}, $timestamptz, $timestamptz)
      RETURNING id, user_id, target_type, target_id, rating, text, created_at, updated_at
    """.query(decoder)

  val selectById: Query[Long, Review] =
    sql"""
      SELECT id, user_id, target_type, target_id, rating, text, created_at, updated_at
      FROM reviews WHERE id = $int8
    """.query(decoder)

  val selectByTarget: Query[(String, Long), Review] =
    sql"""
      SELECT id, user_id, target_type, target_id, rating, text, created_at, updated_at
      FROM reviews
      WHERE target_type = $text AND target_id = $int8
      ORDER BY created_at DESC
    """.query(decoder)

  val selectByUserId: Query[Long, Review] =
    sql"""
      SELECT id, user_id, target_type, target_id, rating, text, created_at, updated_at
      FROM reviews WHERE user_id = $int8
      ORDER BY created_at DESC
    """.query(decoder)

  val updateCommand: Command[(Int, Option[String], Long)] =
    sql"""
      UPDATE reviews
      SET rating = $int4, text = ${text.opt}, updated_at = NOW()
      WHERE id = $int8
    """.command

  val deleteCommand: Command[Long] =
    sql"DELETE FROM reviews WHERE id = $int8".command

  val averageQuery: Query[(String, Long), Double] =
    sql"""
      SELECT COALESCE(AVG(rating), 0.0)::double precision
      FROM reviews
      WHERE target_type = $text AND target_id = $int8
    """.query(float8)

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): ReviewRepository[F] =
    new PostgresReviewRepository(sessionPool)
}