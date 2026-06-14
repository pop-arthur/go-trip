package gotrip.domain

import java.time.Instant
import doobie._
import doobie.postgres.implicits.JavaInstantMeta

sealed trait AchievementConditionType
object AchievementConditionType {
  case object TRIPS_COUNT extends AchievementConditionType
  case object COUNTRIES_COUNT extends AchievementConditionType
  case object ORDERS_COUNT extends AchievementConditionType
  case object REVIEWS_COUNT extends AchievementConditionType
  case object SPENDING_AMOUNT extends AchievementConditionType

  implicit val conditionTypeMeta: Meta[AchievementConditionType] =
    Meta[String].imap {
      case "TRIPS_COUNT"      => TRIPS_COUNT
      case "COUNTRIES_COUNT"  => COUNTRIES_COUNT
      case "ORDERS_COUNT"     => ORDERS_COUNT
      case "REVIEWS_COUNT"    => REVIEWS_COUNT
      case "SPENDING_AMOUNT"  => SPENDING_AMOUNT
    }(_.toString)
}

case class Achievement(
    id: Long,
    code: String,
    title: String,
    description: Option[String],
    conditionType: AchievementConditionType,
    conditionValue: Int,
    iconUrl: Option[String],
    createdAt: Instant,
    updatedAt: Instant
)

object Achievement {
  implicit val achievementRead: Read[Achievement] = Read[(Long, String, String, Option[String], AchievementConditionType, Int, Option[String], Instant, Instant)].map {
    case (id, code, title, desc, condType, condVal, icon, createdAt, updatedAt) =>
      Achievement(id, code, title, desc, condType, condVal, icon, createdAt, updatedAt)
  }
}