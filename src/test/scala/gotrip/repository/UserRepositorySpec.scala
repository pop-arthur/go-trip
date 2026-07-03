package gotrip.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.repository.user.UserRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class UserRepositorySpec extends AnyWordSpec with Matchers with PostgresRepositorySpecBase with RepositoryFixtures:

  "UserRepository" should {
    "create, find, update, delete users and manage roles" in {
      val users = UserRepository.makePostgres[IO](sessionPool)
      val user = sampleUser(1)

      users.create(user).unsafeRunSync() shouldBe user
      users.findById(user.id).unsafeRunSync() shouldBe Some(user)
      users.findByEmail(user.email).unsafeRunSync() shouldBe Some(user)

      val updated = user.copy(email = UserEmail("updated@example.test"), fullName = UserFullName(Some("Updated User")), updatedAt = t(2))
      users.update(updated).unsafeRunSync() shouldBe 1
      users.findByEmail(updated.email).unsafeRunSync() shouldBe Some(updated)

      users.addRole(user.id, Role.USER, t(3), t(3)).unsafeRunSync() shouldBe 1
      users.addRole(user.id, Role.ADMIN, t(4), t(4)).unsafeRunSync() shouldBe 1
      users.getRoles(user.id).unsafeRunSync().toSet shouldBe Set(Role.USER, Role.ADMIN)
      users.removeRole(user.id, Role.USER).unsafeRunSync() shouldBe 1
      users.getRoles(user.id).unsafeRunSync() shouldBe List(Role.ADMIN)

      users.delete(user.id).unsafeRunSync() shouldBe 1
      users.findById(user.id).unsafeRunSync() shouldBe None
    }
  }
