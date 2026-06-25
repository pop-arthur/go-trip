package gotrip.domain.location

import cats.syntax.apply.*
import gotrip.domain.location.*
import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

enum LocationType:
  case Country, City, Airport, TrainStation, BusStation, Port, Hotel, MeetingPoint, Attraction, Other

final case class Location(
  id: LocationId,
  name: LocationName,
  `type`: LocationType,
  country: LocationCountry,
  city: LocationCity,
  address: LocationAddress,
  latitude: LocationLatitude,
  longitude: LocationLongitude
)

final case class LocationSearchParams(
  `type`: Option[LocationType] = None,
  country: Option[String] = None,
  city: Option[String] = None,
  query: Option[String] = None
)

final case class LocationCreate(
  name: LocationName,
  `type`: LocationType,
  country: LocationCountry,
  city: LocationCity,
  address: LocationAddress,
  latitude: LocationLatitude,
  longitude: LocationLongitude
)

object LocationCreate:

  def from(
    name: String,
    locationType: LocationType,
    country: Option[String] = None,
    city: Option[String] = None,
    address: Option[String] = None,
    latitude: Option[Double] = None,
    longitude: Option[Double] = None
  ): Result[LocationCreate] =
    (
      LocationName.from(name),
      LocationCountry.from(country),
      LocationCity.from(city),
      LocationAddress.from(address),
      LocationLatitude.from(latitude),
      LocationLongitude.from(longitude)
    ).mapN { (validName, validCountry, validCity, validAddress, validLatitude, validLongitude) =>
      LocationCreate(
        name = validName,
        `type` = locationType,
        country = validCountry,
        city = validCity,
        address = validAddress,
        latitude = validLatitude,
        longitude = validLongitude
      )
    }

  def validate(location: LocationCreate): Result[LocationCreate] =
    from(
      name = location.name.value,
      locationType = location.`type`,
      country = location.country.value,
      city = location.city.value,
      address = location.address.value,
      latitude = location.latitude.value,
      longitude = location.longitude.value
    )

final case class LocationUpdate(
  name: Option[LocationName] = None,
  `type`: Option[LocationType] = None,
  country: Option[LocationCountry] = None,
  city: Option[LocationCity] = None,
  address: Option[LocationAddress] = None,
  latitude: Option[LocationLatitude] = None,
  longitude: Option[LocationLongitude] = None
)

object LocationUpdate:

  def validate(location: LocationUpdate): Result[LocationUpdate] =
    (
      validateOptional(location.name)(name => LocationName.from(name.value)),
      validateOptional(location.country)(country => LocationCountry.from(country.value)),
      validateOptional(location.city)(city => LocationCity.from(city.value)),
      validateOptional(location.address)(address => LocationAddress.from(address.value)),
      validateOptional(location.latitude)(latitude => LocationLatitude.from(latitude.value)),
      validateOptional(location.longitude)(longitude => LocationLongitude.from(longitude.value))
    ).mapN { (validName, validCountry, validCity, validAddress, validLatitude, validLongitude) =>
      location.copy(
        name = validName,
        country = validCountry,
        city = validCity,
        address = validAddress,
        latitude = validLatitude,
        longitude = validLongitude
      )
    }
