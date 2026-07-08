package gotrip.service.user

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.user._
import gotrip.domain.userrole.Role
import gotrip.repository.user.UserRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class UserServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport {

  "UserService" should {
    "register a new user (delegate to repository.create)" in {
      val repo = mock[UserRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      val email = UserEmail("test@example.com")
      val password = "plain-password"
      val fullName = UserFullName(Some("Test User"))

      val created = User(userId, email, UserPasswordHash(password), fullName, generatedAt, generatedAt)
      expectGeneratedId(generatedData, userId.value)
      expectGeneratedNow(generatedData, generatedAt)
      repo.create.expects(created).returning(IO.pure(user))

      service.register(email, password, fullName).unsafeRunSync() shouldBe user
    }

    "find user by email" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      repo.findByEmail.expects(email).returning(IO.pure(Some(user)))

      service.findByEmail(email).unsafeRunSync() shouldBe Some(user)
    }

    "find user by id" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      repo.findById.expects(userId).returning(IO.pure(Some(user)))

      service.findById(userId).unsafeRunSync() shouldBe Some(user)
    }

    "update user" in {
      val repo = mock[UserRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      val updated = user.copy(fullName = UserFullName(Some("Updated")))
      expectGeneratedNow(generatedData, updatedAt)
      repo.update.expects(updated.copy(updatedAt = updatedAt)).returning(IO.pure(1))

      service.update(updated).unsafeRunSync() shouldBe 1
    }

    "delete user" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      repo.delete.expects(userId).returning(IO.pure(1))

      service.delete(userId).unsafeRunSync() shouldBe 1
    }

    "add role" in {
      val repo = mock[UserRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      expectGeneratedNow(generatedData, generatedAt)
      repo.addRole.expects(userId, Role.ADMIN, generatedAt, generatedAt).returning(IO.pure(1))

      service.addRole(userId, Role.ADMIN).unsafeRunSync() shouldBe 1
    }

    "remove role" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      repo.removeRole.expects(userId, Role.ADMIN).returning(IO.pure(1))

      service.removeRole(userId, Role.ADMIN).unsafeRunSync() shouldBe 1
    }

    "get roles" in {
      val repo = mock[UserRepository[IO]]
      val service = new UserService[IO](repo)

      repo.getRoles.expects(userId).returning(IO.pure(List(Role.USER, Role.ADMIN)))

      service.getRoles(userId).unsafeRunSync() shouldBe List(Role.USER, Role.ADMIN)
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: UserRepository[IO],
    generatedData: GeneratedData[IO]
  ): UserService[IO] =
    given GeneratedData[IO] = generatedData
    new UserService[IO](repository)

  private val userId = UserId(uuid("000000000001"))
  private val email = UserEmail("test@example.com")
  private val generatedAt = Instant.parse("2026-06-01T10:00:00Z")
  private val updatedAt = Instant.parse("2026-06-01T10:05:00Z")
  private val user = User(
    id = userId,
    email = email,
    passwordHash = UserPasswordHash("hashed"),
    fullName = UserFullName(Some("Test User")),
    createdAt = generatedAt,
    updatedAt = generatedAt
  )
}
