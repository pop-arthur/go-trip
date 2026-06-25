package gotrip.service.auth

import cats.effect.Sync
import cats.syntax.applicativeError._
import org.mindrot.jbcrypt.BCrypt

trait PasswordHasher[F[_]]:
  def hash(password: String): F[String]
  def verify(password: String, hash: String): F[Boolean]

object PasswordHasher:
  def bcrypt[F[_]: Sync](cost: Int): PasswordHasher[F] =
    new PasswordHasher[F]:
      override def hash(password: String): F[String] =
        Sync[F].delay(BCrypt.hashpw(password, BCrypt.gensalt(cost)))

      override def verify(password: String, hash: String): F[Boolean] =
        Sync[F].delay(BCrypt.checkpw(password, hash)).handleError(_ => false)
