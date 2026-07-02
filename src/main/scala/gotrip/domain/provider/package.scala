package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName
import java.util.UUID

package object provider {
  opaque type ProviderId = UUID
  object ProviderId {
    def apply(value: UUID): ProviderId = value

    def from(value: UUID): Result[ProviderId] =
      valid(ProviderId(value))
  }
  extension (id: ProviderId) {
    def value: UUID = id
  }

  opaque type ProviderName = String
  object ProviderName {
    def apply(value: String): ProviderName = value

    def from(value: String): Result[ProviderName] =
      validateNonBlank(value, ProviderNameIsBlank)(ProviderName.apply)
  }
  extension (name: ProviderName) {
    @targetName("providerNameValue")
    def value: String = name
  }
}
