package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName

package object provider {
  opaque type ProviderId = Long
  object ProviderId {
    def apply(value: Long): ProviderId = value

    def from(value: Long): Result[ProviderId] =
      validatePositiveLong(value, IdIsNotPositive)(ProviderId.apply)
  }
  extension (id: ProviderId) {
    def value: Long = id
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
