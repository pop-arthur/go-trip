package gotrip.domain

import scala.annotation.targetName
import java.util.UUID

package object notification {
  opaque type NotificationId = UUID
  object NotificationId {
    def apply(value: UUID): NotificationId = value
  }
  extension (id: NotificationId) {
    @targetName("notificationIdValue") def value: UUID = id
  }

  opaque type NotificationUserId = UUID
  object NotificationUserId {
    def apply(value: UUID): NotificationUserId = value
  }
  extension (id: NotificationUserId) {
    @targetName("notificationUserIdValue") def value: UUID = id
  }

  opaque type NotificationOrderId = Option[UUID]
  object NotificationOrderId {
    def apply(value: Option[UUID]): NotificationOrderId = value
  }
  extension (id: NotificationOrderId) {
    @targetName("notificationOrderIdValue") def value: Option[UUID] = id
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
