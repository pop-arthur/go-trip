package gotrip.repository.location

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
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

  override def create(location: LocationCreate): F[Location] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.createQuery).flatMap { query =>
        query.unique(PostgresLocationRepository.toCreateInput(location))
      }
    }

  override def update(id: LocationId, location: LocationUpdate): F[Option[Location]] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.updateQuery).flatMap { query =>
        query.option(PostgresLocationRepository.toUpdateInput(id, location))
      }
    }

  override def delete(id: LocationId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresLocationRepository.deleteQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

object PostgresLocationRepository:
  private type SearchInput = (Option[String], Option[String], Option[String], Option[String])
  private type CreateInput =
    (String, String, Option[String], Option[String], Option[String], Option[Double], Option[Double])
  private type UpdateInput =
    (
      Option[String],
      Option[String],
      Boolean,
      Option[String],
      Boolean,
      Option[String],
      Boolean,
      Option[String],
      Boolean,
      Option[Double],
      Boolean,
      Option[Double],
      Long
    )

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

  val createQuery: Query[CreateInput, Location] =
    sql"""
      insert into locations (name, type, country, city, address, latitude, longitude)
      values ($text, ${text}::location_type, ${text.opt}, ${text.opt}, ${text.opt}, ${float8.opt}, ${float8.opt})
      returning id, name::text, type::text, country::text, city::text, address, latitude, longitude
    """.query(locationDecoder)

  val updateQuery: Query[UpdateInput, Location] =
    sql"""
      update locations
      set name = coalesce(${text.opt}, name),
          type = coalesce(${text.opt}::location_type, type),
          country = case when $bool then ${text.opt} else country end,
          city = case when $bool then ${text.opt} else city end,
          address = case when $bool then ${text.opt} else address end,
          latitude = case when $bool then ${float8.opt} else latitude end,
          longitude = case when $bool then ${float8.opt} else longitude end
      where id = $int8
      returning id, name::text, type::text, country::text, city::text, address, latitude, longitude
    """.query(locationDecoder)

  val deleteQuery: Query[Long, Long] =
    sql"""
      delete from locations
      where id = $int8
      returning id
    """.query(int8)

  private def toCreateInput(location: LocationCreate): CreateInput =
    (
      location.name.value,
      encodeLocationType(location.locationType),
      location.country.value,
      location.city.value,
      location.address.value,
      location.latitude.value,
      location.longitude.value
    )

  private def toUpdateInput(id: LocationId, location: LocationUpdate): UpdateInput =
    (
      location.name.map(_.value),
      location.locationType.map(encodeLocationType),
      location.country.isDefined,
      location.country.flatMap(_.value),
      location.city.isDefined,
      location.city.flatMap(_.value),
      location.address.isDefined,
      location.address.flatMap(_.value),
      location.latitude.isDefined,
      location.latitude.flatMap(_.value),
      location.longitude.isDefined,
      location.longitude.flatMap(_.value),
      id.value
    )
