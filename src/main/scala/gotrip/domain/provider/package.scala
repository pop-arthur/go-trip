package gotrip.domain

import scala.annotation.targetName

package object provider {
  opaque type ProviderId = Long
  object ProviderId {
    def apply(value: Long): ProviderId = value
  }
  extension (id: ProviderId) {
    def value: Long = id
  }

  opaque type ProviderName = String
  object ProviderName {
    def apply(value: String): ProviderName = value
  }
  extension (name: ProviderName) {
    @targetName("providerNameValue")
    def value: String = name
  }
}
