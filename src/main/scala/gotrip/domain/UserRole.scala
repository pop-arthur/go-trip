package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait Role
object Role {
  case object USER extends Role
  case object ADMIN extends Role

  implicit val roleMeta: Meta[Role] =
    Meta[String].imap {
      case "USER"  => USER
      case "ADMIN" => ADMIN
    } {
      case USER  => "USER"
      case ADMIN => "ADMIN"
    }
}

case class UserRole(
    id: Long,
    userId: Long,
    role: Role,
    createdAt: Instant
)

object UserRole {
  implicit val userRoleRead: Read[UserRole] = Read[(Long, Long, Role, Instant)].map {
    case (id, userId, role, createdAt) => UserRole(id, userId, role, createdAt)
  }
}