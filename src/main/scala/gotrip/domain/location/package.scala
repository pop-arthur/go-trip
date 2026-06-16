package gotrip.domain

import scala.annotation.targetName

package object location {
  opaque type LocationId = Long
  object LocationId {
    def apply(value: Long): LocationId = value
  }
  extension (id: LocationId) {
    def value: Long = id
  }

  opaque type LocationName = String
  object LocationName {
    def apply(value: String): LocationName = value
  }
  extension (name: LocationName) {
    def value: String = name
  }

  opaque type LocationCountry = Option[String]
  object LocationCountry {
    def apply(value: Option[String]): LocationCountry = value
  }
  extension (country: LocationCountry) {
    @targetName("locationCountryValue")
    def value: Option[String] = country
  }

  opaque type LocationCity = Option[String]
  object LocationCity {
    def apply(value: Option[String]): LocationCity = value
  }
  extension (city: LocationCity) {
    @targetName("locationCityValue")
    def value: Option[String] = city
  }

  opaque type LocationAddress = Option[String]
  object LocationAddress {
    def apply(value: Option[String]): LocationAddress = value
  }
  extension (address: LocationAddress) {
    @targetName("locationAddressValue")
    def value: Option[String] = address
  }

  opaque type LocationLatitude = Option[Double]
  object LocationLatitude {
    def apply(value: Option[Double]): LocationLatitude = value
  }
  extension (latitude: LocationLatitude) {
    @targetName("locationLatitudeValue")
    def value: Option[Double] = latitude
  }

  opaque type LocationLongitude = Option[Double]
  object LocationLongitude {
    def apply(value: Option[Double]): LocationLongitude = value
  }
  extension (longitude: LocationLongitude) {
    @targetName("locationLongitudeValue")
    def value: Option[Double] = longitude
  }
}
