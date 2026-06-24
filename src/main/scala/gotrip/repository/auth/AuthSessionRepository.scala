package gotrip.repository.auth

import cats.Applicative
import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.auth.AuthSession
import gotrip.domain.user.*
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.collection.mutable
import skunk.data.Completion

trait AuthSessionRepository[F[_]]:
  def create(session: AuthSession): F[AuthSession]
  def findActive(id: UUID, now: Instant): F[Option[AuthSession]]
  def rotate(id: UUID, refreshTokenHash: String, expiresAt: Instant, now: Instant): F[Int]
  def revoke(id: UUID, now: Instant): F[Int]

object AuthSessionRepository:
  def makeInMemory[F[_]: Applicative]: F[AuthSessionRepository[F]] =
    InMemoryAuthSessionRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): AuthSessionRepository[F] =
    PostgresAuthSessionRepository.make(sessionPool)

private object InMemoryAuthSessionRepository:
  def make[F[_]: Applicative]: F[AuthSessionRepository[F]] =
    val state = mutable.Map.empty[UUID, AuthSession]

    val repository = new AuthSessionRepository[F]:
      override def create(session: AuthSession): F[AuthSession] =
        state += (session.id -> session)
        session.pure[F]

      override def findActive(id: UUID, now: Instant): F[Option[AuthSession]] =
        state
          .get(id)
          .filter(session => session.revokedAt.isEmpty && session.expiresAt.isAfter(now))
          .pure[F]

      override def rotate(id: UUID, refreshTokenHash: String, expiresAt: Instant, now: Instant): F[Int] =
        state.get(id) match
          case Some(session) if session.revokedAt.isEmpty =>
            state += (id -> session.copy(
              refreshTokenHash = refreshTokenHash,
              expiresAt = expiresAt,
              updatedAt = now
            ))
            1.pure[F]
          case _ =>
            0.pure[F]

      override def revoke(id: UUID, now: Instant): F[Int] =
        state.get(id) match
          case Some(session) if session.revokedAt.isEmpty =>
            state += (id -> session.copy(revokedAt = Some(now), updatedAt = now))
            1.pure[F]
          case _ =>
            0.pure[F]

    repository.pure[F]

private final class PostgresAuthSessionRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends AuthSessionRepository[F]:

  override def create(session: AuthSession): F[AuthSession] =
    sessionPool.use { db =>
      db.prepare(PostgresAuthSessionRepository.insertQuery).flatMap { query =>
        query.unique(
          (
            session.id,
            session.userId.value,
            session.refreshTokenHash,
            toOffset(session.expiresAt),
            session.revokedAt.map(toOffset),
            toOffset(session.createdAt),
            toOffset(session.updatedAt)
          )
        )
      }
    }

  override def findActive(id: UUID, now: Instant): F[Option[AuthSession]] =
    sessionPool.use { db =>
      db.prepare(PostgresAuthSessionRepository.findActiveQuery).flatMap { query =>
        query.option((id, toOffset(now)))
      }
    }

  override def rotate(id: UUID, refreshTokenHash: String, expiresAt: Instant, now: Instant): F[Int] =
    sessionPool.use { db =>
      db.prepare(PostgresAuthSessionRepository.rotateCommand).flatMap { command =>
        command.execute((refreshTokenHash, toOffset(expiresAt), toOffset(now), id)).map(PostgresAuthSessionRepository.rowsAffected)
      }
    }

  override def revoke(id: UUID, now: Instant): F[Int] =
    sessionPool.use { db =>
      db.prepare(PostgresAuthSessionRepository.revokeCommand).flatMap { command =>
        val revokedAt = toOffset(now)
        command.execute((revokedAt, revokedAt, id)).map(PostgresAuthSessionRepository.rowsAffected)
      }
    }

  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

private object PostgresAuthSessionRepository:
  private def rowsAffected(c: Completion): Int = c match
    case Completion.Insert(count) => count
    case Completion.Update(count) => count
    case Completion.Delete(count) => count
    case _                        => 0

  private val sessionDecoder: Decoder[AuthSession] =
    (uuid ~ int8 ~ text ~ timestamptz ~ timestamptz.opt ~ timestamptz ~ timestamptz).map {
      case id ~ userId ~ refreshTokenHash ~ expiresAt ~ revokedAt ~ createdAt ~ updatedAt =>
        AuthSession(
          id = id,
          userId = UserId(userId),
          refreshTokenHash = refreshTokenHash,
          expiresAt = expiresAt.toInstant,
          revokedAt = revokedAt.map(_.toInstant),
          createdAt = createdAt.toInstant,
          updatedAt = updatedAt.toInstant
        )
    }

  val insertQuery: Query[(UUID, Long, String, OffsetDateTime, Option[OffsetDateTime], OffsetDateTime, OffsetDateTime), AuthSession] =
    sql"""
      INSERT INTO auth_sessions (id, user_id, refresh_token_hash, expires_at, revoked_at, created_at, updated_at)
      VALUES ($uuid, $int8, $text, $timestamptz, ${timestamptz.opt}, $timestamptz, $timestamptz)
      RETURNING id, user_id, refresh_token_hash, expires_at, revoked_at, created_at, updated_at
    """.query(sessionDecoder)

  val findActiveQuery: Query[(UUID, OffsetDateTime), AuthSession] =
    sql"""
      SELECT id, user_id, refresh_token_hash, expires_at, revoked_at, created_at, updated_at
      FROM auth_sessions
      WHERE id = $uuid AND revoked_at IS NULL AND expires_at > $timestamptz
    """.query(sessionDecoder)

  val rotateCommand: Command[(String, OffsetDateTime, OffsetDateTime, UUID)] =
    sql"""
      UPDATE auth_sessions
      SET refresh_token_hash = $text, expires_at = $timestamptz, updated_at = $timestamptz
      WHERE id = $uuid AND revoked_at IS NULL
    """.command

  val revokeCommand: Command[(OffsetDateTime, OffsetDateTime, UUID)] =
    sql"""
      UPDATE auth_sessions
      SET revoked_at = $timestamptz, updated_at = $timestamptz
      WHERE id = $uuid AND revoked_at IS NULL
    """.command

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): AuthSessionRepository[F] =
    new PostgresAuthSessionRepository(sessionPool)
