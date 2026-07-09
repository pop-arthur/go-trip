package gotrip.repository.triplocation

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.SkunkCodecs
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.OffsetDateTime
import java.util.UUID

final class PostgresTripLocationRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends TripLocationRepository[F]:

  override def listByTrip(tripId: TripId): F[List[TripLocation]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.listByTripQuery).flatMap { query =>
        query.stream(tripId.value, 64).compile.toList
      }
    }

  override def findInTrip(tripId: TripId, tripLocationId: TripLocationId): F[Option[TripLocation]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.findInTripQuery).flatMap { query =>
        query.option((tripId.value, tripLocationId.value))
      }
    }

  override def create(
    tripId: TripId,
    location: TripLocationCreate,
    visitOrder: VisitOrder
  ): F[TripLocation] =
    sessionPool.use { session =>
      // Генерируем новый ID
      val newId = UUID.randomUUID()
      session.prepare(PostgresTripLocationRepository.insertQuery).flatMap { cmd =>
        cmd.unique(
          (
            newId,
            tripId.value,
            location.location_id.value,
            visitOrder.value,
            location.arrival_date.value,
            location.departure_date.value
          )
        )
      }
    }

  override def update(
    tripId: TripId,
    tripLocationId: TripLocationId,
    location: TripLocationUpdate
  ): F[Option[TripLocation]] =
    sessionPool.use { session =>
      PostgresTripLocationRepository.updateFragment(tripId, tripLocationId, location) match
        case Some(fragment) =>
          PostgresTripLocationRepository.optionTripLocation(session, fragment)
        case None =>
          session.prepare(PostgresTripLocationRepository.findInTripQuery).flatMap { query =>
            query.option((tripId.value, tripLocationId.value))
          }
    }

  override def delete(tripId: TripId, tripLocationId: TripLocationId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.deleteQuery).flatMap { query =>
        query.option((tripId.value, tripLocationId.value)).map(_.isDefined)
      }
    }

  override def tripExists(tripId: TripId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.tripExistsQuery).flatMap { query =>
        query.option(tripId.value).map(_.isDefined)
      }
    }

  override def tripExistsForUser(userId: UserId, tripId: TripId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.tripExistsForUserQuery).flatMap { query =>
        query.option((userId.value, tripId.value)).map(_.isDefined)
      }
    }

  override def locationExists(locationId: LocationId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.locationExistsQuery).flatMap { query =>
        query.option(locationId.value).map(_.isDefined)
      }
    }

  override def visitOrderExists(
    tripId: TripId,
    visitOrder: VisitOrder,
    excludeTripLocationId: Option[TripLocationId] = None
  ): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.visitOrderExistsQuery).flatMap { query =>
        query.option(PostgresTripLocationRepository.toVisitOrderExistsInput(tripId, visitOrder, excludeTripLocationId))
          .map(_.isDefined)
      }
    }

  override def nextVisitOrder(tripId: TripId): F[VisitOrder] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.nextVisitOrderQuery).flatMap { query =>
        query.unique(tripId.value).map(VisitOrder.apply)
      }
    }

  override def countDistinctCountries(userId: UserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.countDistinctCountriesQuery).flatMap { q =>
        q.unique(userId.value)
      }
    }

object PostgresTripLocationRepository:
  private type VisitOrderExistsInput = (UUID, Int, Boolean, UUID)

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): TripLocationRepository[F] =
    new PostgresTripLocationRepository(sessionPool)

  private def uniqueTripLocation[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[TripLocation] =
    session.prepare(fragment.fragment.query(tripLocationDecoder)).flatMap { query =>
      query.unique(fragment.argument)
    }

  private def optionTripLocation[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Option[TripLocation]] =
    session.prepare(fragment.fragment.query(tripLocationDecoder)).flatMap { query =>
      query.option(fragment.argument)
    }

  private val tripLocationDecoder: Decoder[TripLocation] =
    (uuid ~ uuid ~ uuid ~ int4 ~ timestamptz.opt ~ timestamptz.opt)
      .map {
        case id ~ tripId ~ locationId ~ visitOrder ~ arrivalDate ~ departureDate =>
          TripLocation(
            id = TripLocationId(id),
            trip_id = TripId(tripId),
            location_id = LocationId(locationId),
            visit_order = VisitOrder(visitOrder),
            arrival_date = TripLocationArrivalDate(arrivalDate),
            departure_date = TripLocationDepartureDate(departureDate)
          )
      }

  // ИСПРАВЛЕНО: добавлен id в INSERT (теперь 6 параметров)
  val insertQuery: Query[(UUID, UUID, UUID, Int, Option[OffsetDateTime], Option[OffsetDateTime]), TripLocation] =
    sql"""
      INSERT INTO trip_locations (id, trip_id, location_id, visit_order, arrival_date, departure_date)
      VALUES ($uuid, $uuid, $uuid, $int4, ${timestamptz.opt}, ${timestamptz.opt})
      RETURNING id, trip_id, location_id, visit_order, arrival_date, departure_date
    """.query(tripLocationDecoder)

  val listByTripQuery: Query[UUID, TripLocation] =
    sql"""
      select id, trip_id, location_id, visit_order, arrival_date, departure_date
      from trip_locations
      where trip_id = $uuid
      order by visit_order, id
    """.query(tripLocationDecoder)

  val findInTripQuery: Query[(UUID, UUID), TripLocation] =
    sql"""
      select id, trip_id, location_id, visit_order, arrival_date, departure_date
      from trip_locations
      where trip_id = $uuid
        and id = $uuid
    """.query(tripLocationDecoder)

  val deleteQuery: Query[(UUID, UUID), UUID] =
    sql"""
      delete from trip_locations
      where trip_id = $uuid
        and id = $uuid
      returning id
    """.query(uuid)

  val tripExistsQuery: Query[UUID, UUID] =
    sql"""
      select id
      from trips
      where id = $uuid
    """.query(uuid)

  val tripExistsForUserQuery: Query[(UUID, UUID), UUID] =
    sql"""
      select id
      from trips
      where user_id = $uuid
        and id = $uuid
    """.query(uuid)

  val locationExistsQuery: Query[UUID, UUID] =
    sql"""
      select id
      from locations
      where id = $uuid
    """.query(uuid)

  val visitOrderExistsQuery: Query[VisitOrderExistsInput, UUID] =
    sql"""
      select id
      from trip_locations
      where trip_id = $uuid
        and visit_order = $int4
        and ($bool or id <> $uuid)
      limit 1
    """.query(uuid)

  val nextVisitOrderQuery: Query[UUID, Int] =
    sql"""
      select coalesce(max(visit_order), 0) + 1
      from trip_locations
      where trip_id = $uuid
    """.query(int4)

  val countDistinctCountriesQuery: Query[UUID, Int] =
    sql"""
      SELECT COUNT(DISTINCT l.country)::int
      FROM trips t
      JOIN trip_locations tl ON tl.trip_id = t.id
      JOIN locations l ON l.id = tl.location_id
      WHERE t.user_id = $uuid
        AND l.type = 'COUNTRY'
        AND l.country IS NOT NULL
    """.query(int4)

  private def createFragment(
    tripId: TripId,
    location: TripLocationCreate,
    visitOrder: VisitOrder
  ): AppliedFragment =
    val fields =
      List(
        "trip_id" -> sql"$uuid"(tripId.value),
        "location_id" -> sql"$uuid"(location.location_id.value),
        "visit_order" -> sql"$int4"(visitOrder.value)
      ) ++ List(
        location.arrival_date.value.map(value => "arrival_date" -> sql"$timestamptz"(value)),
        location.departure_date.value.map(value => "departure_date" -> sql"$timestamptz"(value))
      ).flatten

    val columns = fields.map(_._1).mkString(", ")
    val values = combineApplied(fields.map(_._2))
    AppliedFragment(
      sql"""
        insert into trip_locations (#$columns)
        values (${values.fragment})
        returning id, trip_id, location_id, visit_order, arrival_date, departure_date
      """,
      values.argument
    )

  private def updateFragment(
    tripId: TripId,
    tripLocationId: TripLocationId,
    location: TripLocationUpdate
  ): Option[AppliedFragment] =
    val fields =
      List(
        location.visit_order.map(value => sql"visit_order = $int4"(value.value)),
        location.arrival_date.map(value => sql"arrival_date = ${timestamptz.opt}"(value.value)),
        location.departure_date.map(value => sql"departure_date = ${timestamptz.opt}"(value.value))
      ).flatten

    fields.headOption.map { head =>
      val sets = combineApplied(head :: fields.tail)
      AppliedFragment(sql"update trip_locations set ${sets.fragment}", sets.argument) |+|
        sql"""
          where trip_id = $uuid
            and id = $uuid
          returning id, trip_id, location_id, visit_order, arrival_date, departure_date
        """((tripId.value, tripLocationId.value))
    }

  private def toVisitOrderExistsInput(
    tripId: TripId,
    visitOrder: VisitOrder,
    excludeTripLocationId: Option[TripLocationId]
  ): VisitOrderExistsInput =
    (
      tripId.value,
      visitOrder.value,
      excludeTripLocationId.isEmpty,
      excludeTripLocationId.map(_.value).getOrElse(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    )

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }