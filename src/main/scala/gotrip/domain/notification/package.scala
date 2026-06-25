package gotrip.domain

import scala.annotation.targetName

package object notification {
  opaque type NotificationId = Long
  object NotificationId {
    def apply(value: Long): NotificationId = value
  }
  extension (id: NotificationId) {
    @targetName("notificationIdValue") def value: Long = id
  }

  opaque type NotificationUserId = Long
  object NotificationUserId {
    def apply(value: Long): NotificationUserId = value
  }
  extension (id: NotificationUserId) {
    @targetName("notificationUserIdValue") def value: Long = id
  }

  opaque type NotificationOrderId = Option[Long]
  object NotificationOrderId {
    def apply(value: Option[Long]): NotificationOrderId = value
  }
  extension (id: NotificationOrderId) {
    @targetName("notificationOrderIdValue") def value: Option[Long] = id
  }

  opaque type NotificationTitle = String
  object NotificationTitle {
    def apply(value: String): NotificationTitle = value
  }
  extension (title: NotificationTitle) {
    @targetName("notificationTitleValue") def value: String = title
  }

  opaque type NotificationBody = String
  object NotificationBody {
    def apply(value: String): NotificationBody = value
  }
  extension (body: NotificationBody) {
    @targetName("notificationBodyValue") def value: String = body
  }

  sealed trait NotificationType
  object NotificationType {
    case object StatusChange extends NotificationType
    case object Reminder extends NotificationType
    case object General extends NotificationType
    case object Promo extends NotificationType
    case object Other extends NotificationType

    def fromString(s: String): Option[NotificationType] = s match {
      case "STATUS_CHANGE" => Some(StatusChange)
      case "REMINDER"      => Some(Reminder)
      case "GENERAL"       => Some(General)
      case "PROMO"         => Some(Promo)
      case "OTHER"         => Some(Other)
      case _               => None
    }

    def toString(nt: NotificationType): String = nt match {
      case StatusChange => "STATUS_CHANGE"
      case Reminder     => "REMINDER"
      case General      => "GENERAL"
      case Promo        => "PROMO"
      case Other        => "OTHER"
    }
  }
}