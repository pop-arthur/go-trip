package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName
import java.util.UUID

package object additionalservice {
  opaque type ServiceId = UUID
  object ServiceId {
    def apply(value: UUID): ServiceId = value

    def from(value: UUID): Result[ServiceId] =
      valid(ServiceId(value))
  }
  extension (id: ServiceId) {
    def value: UUID = id
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
