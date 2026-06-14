package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

case class User(
    id: Long,
    email: String,
    passwordHash: String,
    fullName: Option[String],
    createdAt: Instant,
    updatedAt: Instant
)

object User {
  implicit val userRead: Read[User] = Read[(Long, String, String, Option[String], Instant, Instant)].map {
    case (id, email, hash, fullName, createdAt, updatedAt) =>
      User(id, email, hash, fullName, createdAt, updatedAt)
  }

  implicit val userWrite: Write[User] = Write[(Long, String, String, Option[String], Instant, Instant)].contramap { u =>
    (u.id, u.email, u.passwordHash, u.fullName, u.createdAt, u.updatedAt)
  }
}