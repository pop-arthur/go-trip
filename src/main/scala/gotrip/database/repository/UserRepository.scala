package gotrip.database.repository

import gotrip.domain.{User, Role}
import cats.effect.MonadCancelThrow
import cats.syntax.all._
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._

trait UserRepository[F[_]] {
  def create(email: String, passwordHash: String, fullName: Option[String]): F[User]
  def findByEmail(email: String): F[Option[User]]
  def findById(id: Long): F[Option[User]]
  def update(user: User): F[Int]
  def delete(id: Long): F[Int]
  // Дополнительно: назначение роли
  def addRole(userId: Long, role: Role): F[Int]
  def getRoles(userId: Long): F[List[Role]]
}

object UserRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): UserRepository[F] =
    new UserRepository[F] {
      def create(email: String, passwordHash: String, fullName: Option[String]): F[User] =
        sql"""
          INSERT INTO users (email, password_hash, full_name)
          VALUES ($email, $passwordHash, $fullName)
          RETURNING id, email, password_hash, full_name, created_at, updated_at
        """.query[User].unique.transact(xa)

      def findByEmail(email: String): F[Option[User]] =
        sql"SELECT id, email, password_hash, full_name, created_at, updated_at FROM users WHERE email = $email"
          .query[User].option.transact(xa)

      def findById(id: Long): F[Option[User]] =
        sql"SELECT id, email, password_hash, full_name, created_at, updated_at FROM users WHERE id = $id"
          .query[User].option.transact(xa)

      def update(user: User): F[Int] =
        sql"""
          UPDATE users
          SET email = ${user.email}, password_hash = ${user.passwordHash}, full_name = ${user.fullName}, updated_at = NOW()
          WHERE id = ${user.id}
        """.update.run.transact(xa)

      def delete(id: Long): F[Int] =
        sql"DELETE FROM users WHERE id = $id".update.run.transact(xa)

      def addRole(userId: Long, role: Role): F[Int] = {
        val roleStr = role match { case Role.USER => "USER" case Role.ADMIN => "ADMIN" }
        sql"INSERT INTO user_roles (user_id, role) VALUES ($userId, $roleStr)".update.run.transact(xa)
      }

      def getRoles(userId: Long): F[List[Role]] =
        sql"SELECT role FROM user_roles WHERE user_id = $userId"
          .query[String].to[List].transact(xa).map(_.map {
            case "USER" => Role.USER
            case "ADMIN" => Role.ADMIN
          })
    }
}