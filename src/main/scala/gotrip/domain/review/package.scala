package gotrip.domain

import scala.annotation.targetName

package object review {
  opaque type ReviewId = Long
  object ReviewId {
    def apply(value: Long): ReviewId = value
  }
  extension (id: ReviewId) {
    @targetName("reviewIdValue") def value: Long = id
  }

  opaque type ReviewRating = Int
  object ReviewRating {
    def apply(value: Int): ReviewRating = value
  }
  extension (rating: ReviewRating) {
    @targetName("reviewRatingValue") def value: Int = rating
  }

  opaque type ReviewText = Option[String]
  object ReviewText {
    def apply(value: Option[String]): ReviewText = value
  }
  extension (text: ReviewText) {
    @targetName("reviewTextValue") def value: Option[String] = text
  }

  opaque type ReviewTargetId = Long
  object ReviewTargetId {
    def apply(value: Long): ReviewTargetId = value
  }
  extension (id: ReviewTargetId) {
    @targetName("reviewTargetIdValue") def value: Long = id
  }

  sealed trait ReviewTargetType
  object ReviewTargetType {
    case object Provider extends ReviewTargetType
    case object Location extends ReviewTargetType
    case object Order extends ReviewTargetType
    case object AdditionalService extends ReviewTargetType

    def fromString(s: String): Option[ReviewTargetType] = s match {
      case "PROVIDER"           => Some(Provider)
      case "LOCATION"           => Some(Location)
      case "ORDER"              => Some(Order)
      case "ADDITIONAL_SERVICE" => Some(AdditionalService)
      case _                    => None
    }

    def toString(tt: ReviewTargetType): String = tt match {
      case Provider          => "PROVIDER"
      case Location          => "LOCATION"
      case Order             => "ORDER"
      case AdditionalService => "ADDITIONAL_SERVICE"
    }
  }
}