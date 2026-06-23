package gotrip.repository.user

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import java.time.Instant
import scala.collection.mutable

object InMemoryUserRepository {
  def make[F[_]: Applicative]: F[UserRepository[F]] = {
    val state = mutable.Map.empty[UserId, User]
    val roles = mutable.Map.empty[UserId, List[Role]]
    var nextId = 1L

    def newId(): UserId = { val id = nextId; nextId += 1; UserId(id) }

    new UserRepository[F] {
      override def create(email: UserEmail, passwordHash: UserPasswordHash, fullName: UserFullName): F[User] = {
        val now = Instant.now()
        val user = User(newId(), email, passwordHash, fullName, now, now)
        state += (user.id -> user)
        roles += (user.id -> List(Role.USER))
        user.pure[F]
      }

      override def findByEmail(email: UserEmail): F[Option[User]] =
        state.values.find(_.email == email).pure[F]

      override def findById(id: UserId): F[Option[User]] =
        state.get(id).pure[F]

      override def update(user: User): F[Int] = {
        state.get(user.id) match {
          case Some(_) =>
            val updated = user.copy(updatedAt = Instant.now())
            state += (user.id -> updated)
            1.pure[F]
          case None => 0.pure[F]
        }
      }

      override def delete(id: UserId): F[Int] = {
        state.remove(id).map(_ => 1).getOrElse(0).pure[F]
      }

      override def addRole(userId: UserId, role: Role): F[Int] = {
        roles.updateWith(userId) {
          case Some(list) if !list.contains(role) => Some(list :+ role)
          case Some(list) => Some(list)
          case None => Some(List(role))
        }
        1.pure[F]
      }

      override def removeRole(userId: UserId, role: Role): F[Int] = {
        roles.updateWith(userId) {
          case Some(list) => Some(list.filterNot(_ == role))
          case None => None
        }
        1.pure[F]
      }

      override def getRoles(userId: UserId): F[List[Role]] =
        roles.getOrElse(userId, List(Role.USER)).pure[F]
    }.pure[F]
  }
}