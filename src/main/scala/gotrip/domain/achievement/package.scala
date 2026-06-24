package gotrip.domain

import scala.annotation.targetName

package object achievement {
  opaque type AchievementId = Long
  object AchievementId {
    def apply(value: Long): AchievementId = value
  }
  extension (id: AchievementId) {
    @targetName("achievementIdValue") def value: Long = id
  }

  opaque type AchievementCode = String
  object AchievementCode {
    def apply(value: String): AchievementCode = value
  }
  extension (code: AchievementCode) {
    @targetName("achievementCodeValue") def value: String = code
  }

  opaque type AchievementTitle = String
  object AchievementTitle {
    def apply(value: String): AchievementTitle = value
  }
  extension (title: AchievementTitle) {
    @targetName("achievementTitleValue") def value: String = title
  }

  opaque type AchievementDescription = Option[String]
  object AchievementDescription {
    def apply(value: Option[String]): AchievementDescription = value
  }
  extension (desc: AchievementDescription) {
    @targetName("achievementDescriptionValue") def value: Option[String] = desc
  }

  opaque type AchievementIconUrl = Option[String]
  object AchievementIconUrl {
    def apply(value: Option[String]): AchievementIconUrl = value
  }
  extension (icon: AchievementIconUrl) {
    @targetName("achievementIconUrlValue") def value: Option[String] = icon
  }

  sealed trait AchievementConditionType
  object AchievementConditionType {
    case object TripsCount extends AchievementConditionType
    case object CountriesCount extends AchievementConditionType
    case object OrdersCount extends AchievementConditionType
    case object ReviewsCount extends AchievementConditionType
    case object SpendingAmount extends AchievementConditionType

    def fromString(s: String): Option[AchievementConditionType] = s match {
      case "TRIPS_COUNT"      => Some(TripsCount)
      case "COUNTRIES_COUNT"  => Some(CountriesCount)
      case "ORDERS_COUNT"     => Some(OrdersCount)
      case "REVIEWS_COUNT"    => Some(ReviewsCount)
      case "SPENDING_AMOUNT"  => Some(SpendingAmount)
      case _                  => None
    }

    def toString(ct: AchievementConditionType): String = ct match {
      case TripsCount      => "TRIPS_COUNT"
      case CountriesCount  => "COUNTRIES_COUNT"
      case OrdersCount     => "ORDERS_COUNT"
      case ReviewsCount    => "REVIEWS_COUNT"
      case SpendingAmount  => "SPENDING_AMOUNT"
    }
  }
}