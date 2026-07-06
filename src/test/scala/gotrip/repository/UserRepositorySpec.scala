package gotrip.repository

import cats.effect.IO
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.repository.user.UserRepository

final class UserRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("UserRepository creates and finds users") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val user = sampleUser(1)

    for
      created <- users.create(user)
      byId <- users.findById(user.id)
      byEmail <- users.findByEmail(user.email)
    yield
      assertEquals(created, user)
      assertEquals(byId, Some(user))
      assertEquals(byEmail, Some(user))
  }

  repositoryTest("UserRepository updates users") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val user = sampleUser(1)
    val updated = user.copy(email = UserEmail("updated@example.test"), fullName = UserFullName(Some("Updated User")), updatedAt = t(2))

    for
      _ <- users.create(user)
      updatedRows <- users.update(updated)
      byUpdatedEmail <- users.findByEmail(updated.email)
    yield
      assertEquals(updatedRows, 1)
      assertEquals(byUpdatedEmail, Some(updated))
  }

  repositoryTest("UserRepository adds, gets, and removes roles") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val user = sampleUser(1)

    for
      _ <- users.create(user)
      userRoleRows <- users.addRole(user.id, Role.USER, t(3), t(3))
      adminRoleRows <- users.addRole(user.id, Role.ADMIN, t(4), t(4))
      roles <- users.getRoles(user.id)
      removedRoleRows <- users.removeRole(user.id, Role.USER)
      rolesAfterRemove <- users.getRoles(user.id)
    yield
      assertEquals(userRoleRows, 1)
      assertEquals(adminRoleRows, 1)
      assertEquals(roles.toSet, Set[Role](Role.USER, Role.ADMIN))
      assertEquals(removedRoleRows, 1)
      assertEquals(rolesAfterRemove, List(Role.ADMIN))
  }

  repositoryTest("UserRepository deletes users") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val user = sampleUser(1)

    for
      _ <- users.create(user)
      deletedRows <- users.delete(user.id)
      byIdAfterDelete <- users.findById(user.id)
    yield
      assertEquals(deletedRows, 1)
      assertEquals(byIdAfterDelete, None)
  }
