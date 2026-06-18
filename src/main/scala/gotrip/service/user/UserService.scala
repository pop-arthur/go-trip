package gotrip.service.user

import gotrip.domain.user._
import gotrip.repository.user.UserRepository
import gotrip.domain.userrole.Role

final class UserService[F[_]](
  repo: UserRepository[F]
):

  def register(email: UserEmail, password: String, fullName: UserFullName): F[User] =
    repo.create(email, UserPasswordHash(password), fullName)

  def findByEmail(email: UserEmail): F[Option[User]] = repo.findByEmail(email)
  def findById(id: UserId): F[Option[User]] = repo.findById(id)
  def update(user: User): F[Int] = repo.update(user)
  def delete(id: UserId): F[Int] = repo.delete(id)

  def addRole(userId: UserId, role: Role): F[Int] = repo.addRole(userId, role)
  def removeRole(userId: UserId, role: Role): F[Int] = repo.removeRole(userId, role)
  def getRoles(userId: UserId): F[List[Role]] = repo.getRoles(userId)