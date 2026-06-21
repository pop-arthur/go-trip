package gotrip.domain

import scala.annotation.targetName

package object additionalservice {
  opaque type ServiceId = Long
  object ServiceId {
    def apply(value: Long): ServiceId = value
  }
  extension (id: ServiceId) {
    def value: Long = id
  }

  opaque type ServiceTitle = String
  object ServiceTitle {
    def apply(value: String): ServiceTitle = value
  }
  extension (title: ServiceTitle) {
    @targetName("serviceTitleValue")
    def value: String = title
  }
}
