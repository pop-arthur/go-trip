package gotrip.service.user

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.user._
import gotrip.repository.user.UserRepository
import gotrip.domain.userrole.Role
import gotrip.service.GeneratedData

final class UserService[F[_]: Sync: Clock: GeneratedData](
  repo: UserRepository[F]
):

  def register(email: UserEmail, password: String, fullName: UserFullName): F[User] =
    for
      id <- GeneratedData[F].newId()
      now <- GeneratedData[F].now()
      user <- repo.create(User(UserId(id), email, UserPasswordHash(password), fullName, now, now))
    yield user

  def findByEmail(email: UserEmail): F[Option[User]] = repo.findByEmail(email)
  def findById(id: UserId): F[Option[User]] = repo.findById(id)
  def update(user: User): F[Int] =
    GeneratedData[F].now().flatMap(now => repo.update(user.copy(updatedAt = now)))
  def delete(id: UserId): F[Int] = repo.delete(id)

  def addRole(userId: UserId, role: Role): F[Int] =
    GeneratedData[F].now().flatMap(now => repo.addRole(userId, role, now, now))
  def removeRole(userId: UserId, role: Role): F[Int] = repo.removeRole(userId, role)
  def getRoles(userId: UserId): F[List[Role]] = repo.getRoles(userId)
