package gotrip.repository.location

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import gotrip.domain.location.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*


final class PostgresLocationRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends LocationRepository[F]:

  override def findById(id: LocationId): F[Option[Location]] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.findByIdQuery).flatMap { query =>
        query.option(id.value)
      }
    }

  override def findAll(): F[List[Location]] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.findAllQuery).flatMap { query =>
        query.stream(Void, 64).compile.toList
      }
    }

  override def search(params: LocationSearchParams): F[List[Location]] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.searchQuery).flatMap { query =>
        query.stream(PostgresLocationRepository.toSearchInput(params), 64).compile.toList
      }
    }


object PostgresLocationRepository:
  private type SearchInput = (Option[String], Option[String], Option[String], Option[String])

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): LocationRepository[F] =
    new PostgresLocationRepository(sessionPool)

  private val locationDecoder: Decoder[Location] =
    (int8 ~ text ~ text ~ text.opt ~ text.opt ~ text.opt ~ float8.opt ~ float8.opt)
      .map {
        case id ~ name ~ locationType ~ country ~ city ~ address ~ latitude ~ longitude =>
          Location(
            id = LocationId(id),
            name = LocationName(name),
            locationType = decodeLocationType(locationType),
            country = LocationCountry(country),
            city = LocationCity(city),
            address = LocationAddress(address),
            latitude = LocationLatitude(latitude),
            longitude = LocationLongitude(longitude)
          )
      }

  val findByIdQuery: Query[Long, Location] =
    sql"""
      select id, name::text, type::text, country::text, city::text, address, latitude, longitude
      from locations
      where id = $int8
    """.query(locationDecoder)

  val findAllQuery: Query[Void, Location] =
    sql"""
      select id, name::text, type::text, country::text, city::text, address, latitude, longitude
      from locations
      order by name
    """.query(locationDecoder)

  val searchQuery: Query[SearchInput, Location] =
    sql"""
      select id, name::text, type::text, country::text, city::text, address, latitude, longitude
      from locations
      where type::text = coalesce(${text.opt}, type::text)
        and lower(country) is not distinct from lower(coalesce(${text.opt}, country::text))
        and lower(city) is not distinct from lower(coalesce(${text.opt}, city::text))
        and lower(name) like coalesce(${text.opt}, lower(name))
      order by name
    """.query(locationDecoder)

  private def toSearchInput(params: LocationSearchParams): SearchInput =
    (
      encodeLocationType(params.locationType),
      params.country,
      params.city,
      params.query.map(query => s"%${query.toLowerCase}%")
    )

  private def encodeLocationType(locationType: Option[LocationType]): Option[String] =
    locationType.map(encodeLocationType)

  private def encodeLocationType(locationType: LocationType): String =
    locationType match
      case LocationType.Country      => "COUNTRY"
      case LocationType.City         => "CITY"
      case LocationType.Airport      => "AIRPORT"
      case LocationType.TrainStation => "TRAIN_STATION"
      case LocationType.BusStation   => "BUS_STATION"
      case LocationType.Port         => "PORT"
      case LocationType.Hotel        => "HOTEL"
      case LocationType.MeetingPoint => "MEETING_POINT"
      case LocationType.Attraction   => "ATTRACTION"
      case LocationType.Other        => "OTHER"

  private def decodeLocationType(value: String): LocationType =
    value match
      case "COUNTRY"       => LocationType.Country
      case "CITY"          => LocationType.City
      case "AIRPORT"       => LocationType.Airport
      case "TRAIN_STATION" => LocationType.TrainStation
      case "BUS_STATION"   => LocationType.BusStation
      case "PORT"          => LocationType.Port
      case "HOTEL"         => LocationType.Hotel
      case "MEETING_POINT" => LocationType.MeetingPoint
      case "ATTRACTION"    => LocationType.Attraction
      case "OTHER"         => LocationType.Other
      case other           => throw new IllegalArgumentException(s"Unknown location type: $other")
