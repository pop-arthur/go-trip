package gotrip.domain.location

enum LocationType:
  case Country, City, Airport, TrainStation, BusStation, Port, Hotel, MeetingPoint, Attraction, Other

final case class Location(
  id: LocationId,
  name: LocationName,
  locationType: LocationType,
  country: LocationCountry,
  city: LocationCity,
  address: LocationAddress,
  latitude: LocationLatitude,
  longitude: LocationLongitude
)

final case class LocationSearchParams(
  locationType: Option[LocationType] = None,
  country: Option[String] = None,
  city: Option[String] = None,
  query: Option[String] = None
)

final case class LocationCreate(
  name: LocationName,
  locationType: LocationType,
  country: LocationCountry,
  city: LocationCity,
  address: LocationAddress,
  latitude: LocationLatitude,
  longitude: LocationLongitude
)

final case class LocationUpdate(
  name: Option[LocationName] = None,
  locationType: Option[LocationType] = None,
  country: Option[LocationCountry] = None,
  city: Option[LocationCity] = None,
  address: Option[LocationAddress] = None,
  latitude: Option[LocationLatitude] = None,
  longitude: Option[LocationLongitude] = None
)