package gotrip.domain

package object userrole {
  opaque type UserRoleId = Long
  object UserRoleId {
    def apply(value: Long): UserRoleId = value
  }
  extension (id: UserRoleId) {
    def value: Long = id
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