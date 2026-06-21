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
      session.prepare(PostgresTripLocationRepository.createQuery).flatMap { query =>
        query.unique(PostgresTripLocationRepository.toCreateInput(tripId, location, visitOrder))
      }
    }

  override def update(
    tripId: TripId,
    tripLocationId: TripLocationId,
    location: TripLocationUpdate
  ): F[Option[TripLocation]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripLocationRepository.updateQuery).flatMap { query =>
        query.option(PostgresTripLocationRepository.toUpdateInput(tripId, tripLocationId, location))
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
  private type CreateInput =
    (Long, Long, Int, Option[OffsetDateTime], Option[OffsetDateTime])
  private type UpdateInput =
    (
      Option[Int],
      Boolean,
      Option[OffsetDateTime],
      Boolean,
      Option[OffsetDateTime],
      Long,
      Long
    )
  private type VisitOrderExistsInput = (Long, Int, Boolean, Long)

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): TripLocationRepository[F] =
    new PostgresTripLocationRepository(sessionPool)

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

  val createQuery: Query[CreateInput, TripLocation] =
    sql"""
      insert into trip_locations (trip_id, location_id, visit_order, arrival_date, departure_date)
      values ($int8, $int8, $int4, ${timestamptz.opt}, ${timestamptz.opt})
      returning id, trip_id, location_id, visit_order, arrival_date, departure_date
    """.query(tripLocationDecoder)

  val updateQuery: Query[UpdateInput, TripLocation] =
    sql"""
      update trip_locations
      set visit_order = coalesce(${int4.opt}, visit_order),
          arrival_date = case when $bool then ${timestamptz.opt} else arrival_date end,
          departure_date = case when $bool then ${timestamptz.opt} else departure_date end
      where trip_id = $int8
        and id = $int8
      returning id, trip_id, location_id, visit_order, arrival_date, departure_date
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

  private def toCreateInput(
    tripId: TripId,
    location: TripLocationCreate,
    visitOrder: VisitOrder
  ): CreateInput =
    (
      tripId.value,
      location.location_id.value,
      visitOrder.value,
      location.arrival_date.value,
      location.departure_date.value
    )

  private def toUpdateInput(
    tripId: TripId,
    tripLocationId: TripLocationId,
    location: TripLocationUpdate
  ): UpdateInput =
    (
      location.visit_order.map(_.value),
      location.arrival_date.isDefined,
      location.arrival_date.flatMap(_.value),
      location.departure_date.isDefined,
      location.departure_date.flatMap(_.value),
      tripId.value,
      tripLocationId.value
    )

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
