package gotrip.domain

import scala.annotation.targetName
import java.util.UUID

package object userrole {
  opaque type UserRoleId = UUID
  object UserRoleId {
    def apply(value: UUID): UserRoleId = value
  }
  extension (id: UserRoleId) {
    @targetName("userRoleIdValue") def value: UUID = id
  }

  sealed trait Role
  object Role {
    case object USER extends Role
    case object ADMIN extends Role

    def fromString(s: String): Option[Role] = s match {
      case "USER"  => Some(USER)
      case "ADMIN" => Some(ADMIN)
      case _       => None
    }

    def toString(role: Role): String = role match {
      case USER  => "USER"
      case ADMIN => "ADMIN"
    }
  }
}
