package gotrip.repository.user

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{Instant, OffsetDateTime, ZoneOffset}

final class PostgresUserRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends UserRepository[F]:

  override def create(user: User): F[User] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.insertQuery).flatMap { cmd =>
        cmd.unique(
          (
            user.id.value,
            user.email.value,
            user.passwordHash.value,
            user.fullName.value,
            PostgresUserRepository.toOffset(user.createdAt),
            PostgresUserRepository.toOffset(user.updatedAt)
          )
        )
      }
    }

  override def findByEmail(email: UserEmail): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.findByEmailQuery).flatMap { query =>
        query.option(email.value)
      }
    }

  override def findById(id: UserId): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.findByIdQuery).flatMap { query =>
        query.option(id.value)
      }
    }

  override def update(user: User): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.updateCommand).flatMap { cmd =>
        cmd.execute(
          (user.email.value, user.passwordHash.value, user.fullName.value, PostgresUserRepository.toOffset(user.updatedAt), user.id.value)
        ).map(PostgresUserRepository.rowsAffected)
      }
    }

  override def delete(id: UserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.deleteCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresUserRepository.rowsAffected)
      }
    }

  override def addRole(userId: UserId, role: Role, createdAt: Instant, updatedAt: Instant): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.addRoleCommand).flatMap { cmd =>
        cmd
          .execute((userId.value, Role.toString(role), PostgresUserRepository.toOffset(createdAt), PostgresUserRepository.toOffset(updatedAt)))
          .map(PostgresUserRepository.rowsAffected)
      }
    }

  override def removeRole(userId: UserId, role: Role): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.removeRoleCommand).flatMap { cmd =>
        cmd.execute((userId.value, Role.toString(role))).map(PostgresUserRepository.rowsAffected)
      }
    }

  override def getRoles(userId: UserId): F[List[Role]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.getRolesQuery).flatMap { query =>
        query.stream(userId.value, 64).compile.toList.map(_.flatMap(Role.fromString))
      }
    }

object PostgresUserRepository {

  private def rowsAffected(c: Completion): Int = c match {
    case Completion.Insert(count) => count
    case Completion.Update(count) => count
    case Completion.Delete(count) => count
    case _                        => 0
  }

  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

  private val userDecoder: Decoder[User] =
    (uuid ~ text ~ text ~ text.opt ~ timestamptz ~ timestamptz).map {
      case id ~ email ~ hash ~ fullName ~ created ~ updated =>
        User(
          id = UserId(id),
          email = UserEmail(email),
          passwordHash = UserPasswordHash(hash),
          fullName = UserFullName(fullName),
          createdAt = created.toInstant,
          updatedAt = updated.toInstant
        )
    }

  val findByEmailQuery: Query[String, User] =
    sql"""
      SELECT id, email::text, password_hash::text, full_name::text, created_at, updated_at
      FROM users WHERE email = $text
    """.query(userDecoder)

  val findByIdQuery: Query[UUID, User] =
    sql"""
      SELECT id, email::text, password_hash::text, full_name::text, created_at, updated_at
      FROM users WHERE id = $uuid
    """.query(userDecoder)

  val insertQuery: Query[(UUID, String, String, Option[String], OffsetDateTime, OffsetDateTime), User] =
    sql"""
      INSERT INTO users (id, email, password_hash, full_name, created_at, updated_at)
      VALUES ($uuid, $text, $text, ${text.opt}, $timestamptz, $timestamptz)
      RETURNING id, email::text, password_hash::text, full_name::text, created_at, updated_at
    """.query(userDecoder)

  val updateCommand: Command[(String, String, Option[String], OffsetDateTime, UUID)] =
    sql"""
      UPDATE users
      SET email = $text, password_hash = $text, full_name = ${text.opt}, updated_at = $timestamptz
      WHERE id = $uuid
    """.command

  val deleteCommand: Command[UUID] =
    sql"DELETE FROM users WHERE id = $uuid".command

  val addRoleCommand: Command[(UUID, String, OffsetDateTime, OffsetDateTime)] =
    sql"""
      INSERT INTO user_roles (user_id, role, created_at, updated_at)
      VALUES ($uuid, $text, $timestamptz, $timestamptz)
      ON CONFLICT (user_id, role) DO NOTHING
    """.command

  val removeRoleCommand: Command[(UUID, String)] =
    sql"DELETE FROM user_roles WHERE user_id = $uuid AND role = $text".command

  val getRolesQuery: Query[UUID, String] =
    sql"SELECT role FROM user_roles WHERE user_id = $uuid".query(text)

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): UserRepository[F] =
    new PostgresUserRepository(sessionPool)
}
