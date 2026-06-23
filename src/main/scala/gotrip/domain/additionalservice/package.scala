package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName

package object additionalservice {
  opaque type ServiceId = Long
  object ServiceId {
    def apply(value: Long): ServiceId = value

    def from(value: Long): Result[ServiceId] =
      validatePositiveLong(value, IdIsNotPositive)(ServiceId.apply)
  }
  extension (id: ServiceId) {
    def value: Long = id
  }

  opaque type ServiceTitle = String
  object ServiceTitle {
    def apply(value: String): ServiceTitle = value

    def from(value: String): Result[ServiceTitle] =
      validateNonBlank(value, AdditionalServiceTitleIsBlank)(ServiceTitle.apply)
  }
  extension (title: ServiceTitle) {
    @targetName("serviceTitleValue")
    def value: String = title
  }
}
