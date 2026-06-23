package gotrip.repository.user

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import skunk._
import skunk.codec.all._
import skunk.implicits._
import skunk.data._
import java.time.{OffsetDateTime, ZoneOffset}

final class PostgresUserRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends UserRepository[F]:

  override def create(email: UserEmail, passwordHash: UserPasswordHash, fullName: UserFullName): F[User] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.insertQuery).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.unique(
          (email.value, passwordHash.value, fullName.value, now, now)
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
          (user.email.value, user.passwordHash.value, user.fullName.value, user.id.value)
        ).map(PostgresUserRepository.rowsAffected)
      }
    }

  override def delete(id: UserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.deleteCommand).flatMap { cmd =>
        cmd.execute(id.value).map(PostgresUserRepository.rowsAffected)
      }
    }

  override def addRole(userId: UserId, role: Role): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.addRoleCommand).flatMap { cmd =>
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        cmd.execute((userId.value, Role.toString(role), now, now)).map(PostgresUserRepository.rowsAffected)
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

  private val userDecoder: Decoder[User] =
    (int8 ~ text ~ text ~ text.opt ~ timestamptz ~ timestamptz).map {
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

  val insertQuery: Query[(String, String, Option[String], OffsetDateTime, OffsetDateTime), User] =
    sql"""
      INSERT INTO users (email, password_hash, full_name, created_at, updated_at)
      VALUES ($text, $text, ${text.opt}, $timestamptz, $timestamptz)
      RETURNING id, email, password_hash, full_name, created_at, updated_at
    """.query(userDecoder)

  val findByEmailQuery: Query[String, User] =
    sql"""
      SELECT id, email, password_hash, full_name, created_at, updated_at
      FROM users WHERE email = $text
    """.query(userDecoder)

  val findByIdQuery: Query[Long, User] =
    sql"""
      SELECT id, email, password_hash, full_name, created_at, updated_at
      FROM users WHERE id = $int8
    """.query(userDecoder)

  val updateCommand: Command[(String, String, Option[String], Long)] =
    sql"""
      UPDATE users
      SET email = $text, password_hash = $text, full_name = ${text.opt}, updated_at = NOW()
      WHERE id = $int8
    """.command

  val deleteCommand: Command[Long] =
    sql"DELETE FROM users WHERE id = $int8".command

  val addRoleCommand: Command[(Long, String, OffsetDateTime, OffsetDateTime)] =
    sql"""
      INSERT INTO user_roles (user_id, role, created_at, updated_at)
      VALUES ($int8, $text, $timestamptz, $timestamptz)
    """.command

  val removeRoleCommand: Command[(Long, String)] =
    sql"DELETE FROM user_roles WHERE user_id = $int8 AND role = $text".command

  val getRolesQuery: Query[Long, String] =
    sql"SELECT role FROM user_roles WHERE user_id = $int8".query(text)

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): UserRepository[F] =
    new PostgresUserRepository(sessionPool)
}