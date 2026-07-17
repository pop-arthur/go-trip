package gotrip.service.user

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.repository.user.UserRepository
import gotrip.service.GeneratedData

trait UserService[F[_]] {
  def register(email: UserEmail, password: String, fullName: UserFullName): F[User]
  def findByEmail(email: UserEmail): F[Option[User]]
  def findById(id: UserId): F[Option[User]]
  def update(user: User): F[Int]
  def delete(id: UserId): F[Int]
  def addRole(userId: UserId, role: Role): F[Int]
  def removeRole(userId: UserId, role: Role): F[Int]
  def getRoles(userId: UserId): F[List[Role]]
}

object UserService {
  def make[F[_]: Sync: Clock: GeneratedData](repo: UserRepository[F]): UserService[F] =
    new UserService[F] {
      override def register(email: UserEmail, password: String, fullName: UserFullName): F[User] =
        for {
          id <- GeneratedData[F].newId()
          now <- GeneratedData[F].now()
          user <- repo.create(User(UserId(id), email, UserPasswordHash(password), fullName, now, now))
        } yield user

      override def findByEmail(email: UserEmail): F[Option[User]] = repo.findByEmail(email)
      override def findById(id: UserId): F[Option[User]] = repo.findById(id)
      override def update(user: User): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.update(user.copy(updatedAt = now)))
      override def delete(id: UserId): F[Int] = repo.delete(id)
      override def addRole(userId: UserId, role: Role): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.addRole(userId, role, now, now))
      override def removeRole(userId: UserId, role: Role): F[Int] = repo.removeRole(userId, role)
      override def getRoles(userId: UserId): F[List[Role]] = repo.getRoles(userId)
    }
}