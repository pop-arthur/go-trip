package gotrip.repository.location

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.all.*
import gotrip.domain.location.*
import gotrip.repository.SkunkCodecs.locationType
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

  override def create(location: Location): F[Location] =
    sessionPool.use { session =>
      PostgresLocationRepository.uniqueLocation(session, PostgresLocationRepository.createFragment(location))
    }

  override def update(id: LocationId, location: LocationUpdate): F[Option[Location]] =
    sessionPool.use { session =>
      PostgresLocationRepository.updateFragment(id, location) match
        case Some(fragment) =>
          PostgresLocationRepository.optionLocation(session, fragment)
        case None =>
          session.prepare(PostgresLocationRepository.findByIdQuery).flatMap { query =>
            query.option(id.value)
          }
      }

  override def delete(id: LocationId): F[Boolean] =
    sessionPool.use { session =>
      session.transaction.use { _ =>
        session.prepare(PostgresLocationRepository.findByIdQuery).flatMap { query =>
          query.option(id.value).flatMap {
            case None =>
              false.pure[F]
            case Some(_) =>
              PostgresLocationRepository.deleteDependents(session, id) *>
                session.prepare(PostgresLocationRepository.deleteQuery).flatMap { delete =>
                  delete.option(id.value).map(_.isDefined)
                }
          }
        }
      }
    }

object PostgresLocationRepository:
  private type SearchInput = (Option[LocationType], Option[String], Option[String], Option[String])

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): LocationRepository[F] =
    new PostgresLocationRepository(sessionPool)

  private def uniqueLocation[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Location] =
    session.prepare(fragment.fragment.query(locationDecoder)).flatMap { query =>
      query.unique(fragment.argument)
    }

  private def optionLocation[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Option[Location]] =
    session.prepare(fragment.fragment.query(locationDecoder)).flatMap { query =>
      query.option(fragment.argument)
    }

  private val locationDecoder: Decoder[Location] =
    (uuid ~ text ~ locationType ~ text.opt ~ text.opt ~ text.opt ~ float8.opt ~ float8.opt)
      .map {
        case id ~ name ~ decodedLocationType ~ country ~ city ~ address ~ latitude ~ longitude =>
          Location(
            id = LocationId(id),
            name = LocationName(name),
            `type` = decodedLocationType,
            country = LocationCountry(country),
            city = LocationCity(city),
            address = LocationAddress(address),
            latitude = LocationLatitude(latitude),
            longitude = LocationLongitude(longitude)
          )
      }

  val findByIdQuery: Query[UUID, Location] =
    sql"""
      select id, name::text, type, country::text, city::text, address, latitude, longitude
      from locations
      where id = $uuid
    """.query(locationDecoder)

  val findAllQuery: Query[Void, Location] =
    sql"""
      select id, name::text, type, country::text, city::text, address, latitude, longitude
      from locations
      order by name
    """.query(locationDecoder)

  val searchQuery: Query[SearchInput, Location] =
    sql"""
      select id, name::text, type, country::text, city::text, address, latitude, longitude
      from locations
      where type = coalesce(${locationType.opt}, type)
        and lower(country) is not distinct from lower(coalesce(${text.opt}, country::text))
        and lower(city) is not distinct from lower(coalesce(${text.opt}, city::text))
        and lower(name) like coalesce(${text.opt}, lower(name))
      order by name
    """.query(locationDecoder)

  private def toSearchInput(params: LocationSearchParams): SearchInput =
    (
      params.`type`,
      params.country,
      params.city,
      params.query.map(query => s"%${query.toLowerCase}%")
    )

  val deleteQuery: Query[UUID, UUID] =
    sql"""
      delete from locations
      where id = $uuid
      returning id
    """.query(uuid)

  val deleteTripLocationsCommand: Command[UUID] =
    sql"""
      delete from trip_locations
      where location_id = $uuid
    """.command

  val clearOrderLocationReferencesCommand: Command[(UUID, UUID, UUID, UUID)] =
    sql"""
      update orders
      set
        departure_location_id = case when departure_location_id = $uuid then null else departure_location_id end,
        arrival_location_id = case when arrival_location_id = $uuid then null else arrival_location_id end
      where departure_location_id = $uuid or arrival_location_id = $uuid
    """.command

  val clearAdditionalServiceLocationReferencesCommand: Command[UUID] =
    sql"""
      update additional_services
      set location_id = null
      where location_id = $uuid
    """.command

  val deleteLocationReviewsCommand: Command[UUID] =
    sql"""
      delete from reviews
      where target_type = 'LOCATION' and target_id = $uuid
    """.command

  private def deleteDependents[F[_]: Concurrent](
    session: Session[F],
    id: LocationId
  ): F[Unit] =
    for
      deleteReviews <- session.prepare(deleteLocationReviewsCommand)
      clearServices <- session.prepare(clearAdditionalServiceLocationReferencesCommand)
      clearOrders <- session.prepare(clearOrderLocationReferencesCommand)
      deleteTripLocations <- session.prepare(deleteTripLocationsCommand)
      _ <- deleteReviews.execute(id.value)
      _ <- clearServices.execute(id.value)
      _ <- clearOrders.execute((id.value, id.value, id.value, id.value))
      _ <- deleteTripLocations.execute(id.value)
    yield ()

  private def createFragment(location: Location): AppliedFragment =
    val fields =
      List(
        "id" -> sql"$uuid"(location.id.value),
        "name" -> sql"$text"(location.name.value),
        "type" -> sql"$locationType"(location.`type`)
      ) ++ List(
        location.country.value.map(value => "country" -> sql"$text"(value)),
        location.city.value.map(value => "city" -> sql"$text"(value)),
        location.address.value.map(value => "address" -> sql"$text"(value)),
        location.latitude.value.map(value => "latitude" -> sql"$float8"(value)),
        location.longitude.value.map(value => "longitude" -> sql"$float8"(value))
      ).flatten

    val columns = fields.map(_._1).mkString(", ")
    val values = combineApplied(fields.map(_._2))
    AppliedFragment(
      sql"""
        insert into locations (#$columns)
        values (${values.fragment})
        returning id, name::text, type, country::text, city::text, address, latitude, longitude
      """,
      values.argument
    )

  private def updateFragment(id: LocationId, location: LocationUpdate): Option[AppliedFragment] =
    val fields =
      List(
        location.name.map(value => sql"name = $text"(value.value)),
        location.`type`.map(value => sql"type = $locationType"(value)),
        location.country.map(value => sql"country = ${text.opt}"(value.value)),
        location.city.map(value => sql"city = ${text.opt}"(value.value)),
        location.address.map(value => sql"address = ${text.opt}"(value.value)),
        location.latitude.map(value => sql"latitude = ${float8.opt}"(value.value)),
        location.longitude.map(value => sql"longitude = ${float8.opt}"(value.value))
      ).flatten

    fields.headOption.map { head =>
      val sets = combineApplied(head :: fields.tail)
      AppliedFragment(sql"update locations set ${sets.fragment}", sets.argument) |+|
        sql"""
          where id = $uuid
          returning id, name::text, type, country::text, city::text, address, latitude, longitude
        """(id.value)
    }

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }
