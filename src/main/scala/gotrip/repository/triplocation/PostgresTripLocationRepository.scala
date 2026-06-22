package gotrip.repository.triplocation

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.location.*
import gotrip.domain.trip.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.OffsetDateTime

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
      PostgresTripLocationRepository.uniqueTripLocation(
        session,
        PostgresTripLocationRepository.createFragment(tripId, location, visitOrder)
      )
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

object PostgresTripLocationRepository:
  private type VisitOrderExistsInput = (Long, Int, Boolean, Long)

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
    (int8 ~ int8 ~ int8 ~ int4 ~ timestamptz.opt ~ timestamptz.opt)
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

  val listByTripQuery: Query[Long, TripLocation] =
    sql"""
      select id, trip_id, location_id, visit_order, arrival_date, departure_date
      from trip_locations
      where trip_id = $int8
      order by visit_order, id
    """.query(tripLocationDecoder)

  val findInTripQuery: Query[(Long, Long), TripLocation] =
    sql"""
      select id, trip_id, location_id, visit_order, arrival_date, departure_date
      from trip_locations
      where trip_id = $int8
        and id = $int8
    """.query(tripLocationDecoder)

  val deleteQuery: Query[(Long, Long), Long] =
    sql"""
      delete from trip_locations
      where trip_id = $int8
        and id = $int8
      returning id
    """.query(int8)

  val tripExistsQuery: Query[Long, Long] =
    sql"""
      select id
      from trips
      where id = $int8
    """.query(int8)

  val locationExistsQuery: Query[Long, Long] =
    sql"""
      select id
      from locations
      where id = $int8
    """.query(int8)

  val visitOrderExistsQuery: Query[VisitOrderExistsInput, Long] =
    sql"""
      select id
      from trip_locations
      where trip_id = $int8
        and visit_order = $int4
        and ($bool or id <> $int8)
      limit 1
    """.query(int8)

  val nextVisitOrderQuery: Query[Long, Int] =
    sql"""
      select coalesce(max(visit_order), 0) + 1
      from trip_locations
      where trip_id = $int8
    """.query(int4)

  private def createFragment(
    tripId: TripId,
    location: TripLocationCreate,
    visitOrder: VisitOrder
  ): AppliedFragment =
    val fields =
      List(
        "trip_id" -> sql"$int8"(tripId.value),
        "location_id" -> sql"$int8"(location.location_id.value),
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
          where trip_id = $int8
            and id = $int8
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
      excludeTripLocationId.map(_.value).getOrElse(0L)
    )

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }
