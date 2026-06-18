package gotrip.repository.user

import gotrip.domain.user.{User, UserEmail, UserId, UserPasswordHash, UserFullName}
import gotrip.domain.userrole.Role
import cats.effect.{Concurrent, Resource}
import cats.Id
import skunk.Session

trait UserRepository[F[_]]:
  def create(email: UserEmail, passwordHash: UserPasswordHash, fullName: UserFullName): F[User]
  def findByEmail(email: UserEmail): F[Option[User]]
  def findById(id: UserId): F[Option[User]]
  def update(user: User): F[Int]
  def delete(id: UserId): F[Int]

  def addRole(userId: UserId, role: Role): F[Int]
  def removeRole(userId: UserId, role: Role): F[Int]
  def getRoles(userId: UserId): F[List[Role]]

object UserRepository:
  def makeInMemory[F[_]: Applicative]: F[UserRepository[F]] =
    InMemoryUserRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): UserRepository[F] =
    PostgresUserRepository.make(sessionPool)