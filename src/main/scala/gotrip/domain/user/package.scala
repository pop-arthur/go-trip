package gotrip.domain

import scala.annotation.targetName
import java.util.UUID

package object user {
  opaque type UserId = UUID
  object UserId {
    def apply(value: UUID): UserId = value
  }
  extension (id: UserId) {
    @targetName("userIdValue") def value: UUID = id
  }

  opaque type UserEmail = String
  object UserEmail {
    def apply(value: String): UserEmail = value
  }
  extension (email: UserEmail) {
    @targetName("userEmailValue") def value: String = email
  }

  opaque type UserPasswordHash = String
  object UserPasswordHash {
    def apply(value: String): UserPasswordHash = value
  }
  extension (hash: UserPasswordHash) {
    @targetName("userPasswordHashValue") def value: String = hash
  }

  opaque type UserFullName = Option[String]
  object UserFullName {
    def apply(value: Option[String]): UserFullName = value
  }
  extension (name: UserFullName) {
    @targetName("userFullNameValue") def value: Option[String] = name
  }
}
