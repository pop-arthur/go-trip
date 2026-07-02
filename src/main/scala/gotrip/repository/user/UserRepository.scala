package gotrip.repository.user

import cats.Applicative
import cats.effect.Concurrent
import cats.effect.Resource
import gotrip.domain.user.{User, UserEmail, UserId}
import gotrip.domain.userrole.Role
import skunk.Session
import java.time.Instant

trait UserRepository[F[_]]:
  def create(user: User): F[User]
  def findByEmail(email: UserEmail): F[Option[User]]
  def findById(id: UserId): F[Option[User]]
  def update(user: User): F[Int]
  def delete(id: UserId): F[Int]
  def addRole(userId: UserId, role: Role, createdAt: Instant, updatedAt: Instant): F[Int]
  def removeRole(userId: UserId, role: Role): F[Int]
  def getRoles(userId: UserId): F[List[Role]]

object UserRepository:
  def makeInMemory[F[_]: Applicative]: F[UserRepository[F]] =
    InMemoryUserRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): UserRepository[F] =
    PostgresUserRepository.make(sessionPool)
