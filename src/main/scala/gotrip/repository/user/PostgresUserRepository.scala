package gotrip.repository.user

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import skunk._
import skunk.codec.all._
import skunk.implicits._
import java.time.Instant

final class PostgresUserRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends UserRepository[F]:

  override def create(email: UserEmail, passwordHash: UserPasswordHash, fullName: UserFullName): F[User] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.insertQuery).flatMap { cmd =>
        cmd.unique(
          (email.value, passwordHash.value, fullName.value, Instant.now(), Instant.now())
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
      session.prepare(PostgresUserRepository.updateQuery).flatMap { cmd =>
        cmd.execute(
          (user.email.value, user.passwordHash.value, user.fullName.value, user.id.value)
        ).map(_.rows)
      }
    }

  override def delete(id: UserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.deleteQuery).flatMap { cmd =>
        cmd.execute(id.value).map(_.rows)
      }
    }

  override def addRole(userId: UserId, role: Role): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.addRoleQuery).flatMap { cmd =>
        cmd.execute((userId.value, Role.toString(role), Instant.now(), Instant.now())).map(_.rows)
      }
    }

  override def removeRole(userId: UserId, role: Role): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.removeRoleQuery).flatMap { cmd =>
        cmd.execute((userId.value, Role.toString(role))).map(_.rows)
      }
    }

  override def getRoles(userId: UserId): F[List[Role]] =
    sessionPool.use { session =>
      session.prepare(PostgresUserRepository.getRolesQuery).flatMap { query =>
        query.stream(userId.value, 64).compile.toList.map(_.flatMap(Role.fromString))
      }
    }

object PostgresUserRepository:

  private val userDecoder: Decoder[User] =
    (int8 ~ text ~ text ~ text.opt ~ timestamptz ~ timestamptz).map {
      case id ~ email ~ hash ~ fullName ~ created ~ updated =>
        User(
          id = UserId(id),
          email = UserEmail(email),
          passwordHash = UserPasswordHash(hash),
          fullName = UserFullName(fullName),
          createdAt = created,
          updatedAt = updated
        )
    }

  val insertQuery: Command[(String, String, Option[String], Instant, Instant)] =
    sql"""
      INSERT INTO users (email, password_hash, full_name, created_at, updated_at)
      VALUES ($text, $text, ${text.opt}, $timestamptz, $timestamptz)
      RETURNING id, email, password_hash, full_name, created_at, updated_at
    """.query(userDecoder).command

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

  val updateQuery: Command[(String, String, Option[String], Long)] =
    sql"""
      UPDATE users
      SET email = $text, password_hash = $text, full_name = ${text.opt}, updated_at = NOW()
      WHERE id = $int8
    """.command

  val deleteQuery: Command[Long] =
    sql"DELETE FROM users WHERE id = $int8".command

  val addRoleQuery: Command[(Long, String, Instant, Instant)] =
    sql"""
      INSERT INTO user_roles (user_id, role, created_at, updated_at)
      VALUES ($int8, $text, $timestamptz, $timestamptz)
    """.command

  val removeRoleQuery: Command[(Long, String)] =
    sql"DELETE FROM user_roles WHERE user_id = $int8 AND role = $text".command

  val getRolesQuery: Query[Long, String] =
    sql"SELECT role FROM user_roles WHERE user_id = $int8".query(text)

  def make[F[_]: Concurrent](sessionPool: Resource[F, Session[F]]): UserRepository[F] =
    new PostgresUserRepository(sessionPool)