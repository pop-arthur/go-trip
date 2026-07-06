package gotrip.service.user

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import gotrip.repository.user.UserRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class UserServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  "UserService" should {
    "register a new user (delegate to repository.create)" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      val email = UserEmail("test@example.com")
      val password = "plain-password"
      val fullName = UserFullName(Some("Test User"))

      (repo.create _).expects(where { (u: User) =>
        u.email == email && u.passwordHash == UserPasswordHash(password) && u.fullName == fullName
      }).returning(IO.pure(user))

      service.register(email, password, fullName).unsafeRunSync() shouldBe user
    }

    "find user by email" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      (repo.findByEmail _).expects(email).returning(IO.pure(Some(user)))

      service.findByEmail(email).unsafeRunSync() shouldBe Some(user)
    }

    "find user by id" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      (repo.findById _).expects(userId).returning(IO.pure(Some(user)))

      service.findById(userId).unsafeRunSync() shouldBe Some(user)
    }

    "update user" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      val updated = user.copy(fullName = UserFullName(Some("Updated")))
      (repo.update _).expects(where { (u: User) =>
        u.id == userId &&
        u.email == email &&
        u.passwordHash == UserPasswordHash("hashed") &&
        u.fullName == UserFullName(Some("Updated"))
      }).returning(IO.pure(1))

      service.update(updated).unsafeRunSync() shouldBe 1
    }

    "delete user" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      (repo.delete _).expects(userId).returning(IO.pure(1))

      service.delete(userId).unsafeRunSync() shouldBe 1
    }

    "add role" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      val now = Instant.now()
      (repo.addRole _).expects(where { (uid: UserId, role: Role, c: Instant, u: Instant) =>
        uid == userId && role == Role.ADMIN
      }).returning(IO.pure(1))

      service.addRole(userId, Role.ADMIN).unsafeRunSync() shouldBe 1
    }

    "remove role" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      (repo.removeRole _).expects(where { (uid: UserId, role: Role) =>
        uid == userId && role == Role.ADMIN
      }).returning(IO.pure(1))

      service.removeRole(userId, Role.ADMIN).unsafeRunSync() shouldBe 1
    }

    "get roles" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      (repo.getRoles _).expects(userId).returning(IO.pure(List(Role.USER, Role.ADMIN)))

      service.getRoles(userId).unsafeRunSync() shouldBe List(Role.USER, Role.ADMIN)
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = UserId(uuid("000000000001"))
  private val email = UserEmail("test@example.com")
  private val user = User(
    id = userId,
    email = email,
    passwordHash = UserPasswordHash("hashed"),
    fullName = UserFullName(Some("Test User")),
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )
}