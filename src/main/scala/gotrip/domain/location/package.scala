package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName

package object location {
  opaque type LocationId = Long
  object LocationId {
    def apply(value: Long): LocationId = value

    def from(value: Long): Result[LocationId] =
      validatePositiveLong(value, IdIsNotPositive)(LocationId.apply)
  }
  extension (id: LocationId) {
    def value: Long = id
  }

  opaque type LocationName = String
  object LocationName {
    def apply(value: String): LocationName = value

    def from(value: String): Result[LocationName] =
      validateNonBlank(value, LocationNameIsBlank)(LocationName.apply)
  }
  extension (name: LocationName) {
    def value: String = name
  }

  opaque type LocationCountry = Option[String]
  object LocationCountry {
    def apply(value: Option[String]): LocationCountry = value

    def from(value: Option[String]): Result[LocationCountry] =
      valid(LocationCountry(value))
  }
  extension (country: LocationCountry) {
    @targetName("locationCountryValue")
    def value: Option[String] = country
  }

  opaque type LocationCity = Option[String]
  object LocationCity {
    def apply(value: Option[String]): LocationCity = value

    def from(value: Option[String]): Result[LocationCity] =
      valid(LocationCity(value))
  }
  extension (city: LocationCity) {
    @targetName("locationCityValue")
    def value: Option[String] = city
  }

  opaque type LocationAddress = Option[String]
  object LocationAddress {
    def apply(value: Option[String]): LocationAddress = value

    def from(value: Option[String]): Result[LocationAddress] =
      valid(LocationAddress(value))
  }
  extension (address: LocationAddress) {
    @targetName("locationAddressValue")
    def value: Option[String] = address
  }

  opaque type LocationLatitude = Option[Double]
  object LocationLatitude {
    def apply(value: Option[Double]): LocationLatitude = value

    def from(value: Option[Double]): Result[LocationLatitude] =
      validateOptionalDoubleRange(value, -90.0, 90.0, LatitudeOutOfRange)(LocationLatitude.apply)
  }
  extension (latitude: LocationLatitude) {
    @targetName("locationLatitudeValue")
    def value: Option[Double] = latitude
  }

  opaque type LocationLongitude = Option[Double]
  object LocationLongitude {
    def apply(value: Option[Double]): LocationLongitude = value

    def from(value: Option[Double]): Result[LocationLongitude] =
      validateOptionalDoubleRange(value, -180.0, 180.0, LongitudeOutOfRange)(LocationLongitude.apply)
  }
  extension (longitude: LocationLongitude) {
    @targetName("locationLongitudeValue")
    def value: Option[Double] = longitude
  }
}
