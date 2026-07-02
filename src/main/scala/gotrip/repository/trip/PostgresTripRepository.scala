package gotrip.repository.trip

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.SkunkCodecs
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}

final class PostgresTripRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends TripRepository[F]:

  override def listByUser(userId: UserId, params: TripSearchParams): F[List[Trip]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.listByUserQuery).flatMap { query =>
        query.stream(PostgresTripRepository.toListInput(userId, params), 64).compile.toList
      }
    }

  override def findByUser(userId: UserId, tripId: TripId): F[Option[Trip]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.findByUserQuery).flatMap { query =>
        query.option((userId.value, tripId.value))
      }
    }

  override def create(trip: Trip): F[Trip] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.createQuery).flatMap { query =>
        query.unique(
          (
            trip.id.value,
            trip.user_id.value,
            trip.title.value,
            trip.start_date.value,
            trip.end_date.value,
            trip.status,
            PostgresTripRepository.toOffset(trip.created_at),
            PostgresTripRepository.toOffset(trip.updated_at)
          )
        )
      }
    }

  override def update(trip: Trip): F[Option[Trip]] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.updateQuery).flatMap { query =>
        query.option(
          (
            trip.title.value,
            trip.start_date.value,
            trip.end_date.value,
            trip.status,
            PostgresTripRepository.toOffset(trip.updated_at),
            trip.user_id.value,
            trip.id.value
          )
        )
      }
    }

  override def delete(userId: UserId, tripId: TripId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.deleteQuery).flatMap { query =>
        query.option((userId.value, tripId.value)).map(_.isDefined)
      }
    }

  override def existsForUser(userId: UserId, tripId: TripId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.existsForUserQuery).flatMap { query =>
        query.option((userId.value, tripId.value)).map(_.isDefined)
      }
    }

object PostgresTripRepository:
  private type ListInput = (
    UUID,
    Boolean,
    TripStatus,
    Boolean,
    LocalDate,
    Boolean,
    LocalDate
  )

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): TripRepository[F] =
    new PostgresTripRepository(sessionPool)

  private val tripDecoder: Decoder[Trip] =
    (uuid ~ uuid ~ text ~ date.opt ~ date.opt ~ SkunkCodecs.tripStatus ~ timestamptz ~ timestamptz)
      .map {
        case id ~ userId ~ title ~ startDate ~ endDate ~ status ~ createdAt ~ updatedAt =>
          Trip(
            id = TripId(id),
            user_id = UserId(userId),
            title = TripTitle(title),
            start_date = TripStartDate(startDate),
            end_date = TripEndDate(endDate),
            status = status,
            created_at = createdAt.toInstant,
            updated_at = updatedAt.toInstant
          )
      }

  val listByUserQuery: Query[ListInput, Trip] =
    sql"""
      select id, user_id, title::text, start_date, end_date, status, created_at, updated_at
      from trips
      where user_id = $uuid
        and ($bool or status = ${SkunkCodecs.tripStatus})
        and ($bool or start_date >= $date)
        and ($bool or end_date <= $date)
      order by coalesce(start_date, date '9999-12-31'), id
    """.query(tripDecoder)

  val findByUserQuery: Query[(UUID, UUID), Trip] =
    sql"""
      select id, user_id, title::text, start_date, end_date, status, created_at, updated_at
      from trips
      where user_id = $uuid
        and id = $uuid
    """.query(tripDecoder)

  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

  val createQuery: Query[(UUID, UUID, String, Option[LocalDate], Option[LocalDate], TripStatus, OffsetDateTime, OffsetDateTime), Trip] =
    sql"""
      insert into trips (id, user_id, title, start_date, end_date, status, created_at, updated_at)
      values ($uuid, $uuid, $text, ${date.opt}, ${date.opt}, ${SkunkCodecs.tripStatus}, $timestamptz, $timestamptz)
      returning id, user_id, title::text, start_date, end_date, status, created_at, updated_at
    """.query(tripDecoder)

  val updateQuery: Query[(String, Option[LocalDate], Option[LocalDate], TripStatus, OffsetDateTime, UUID, UUID), Trip] =
    sql"""
      update trips
      set title = $text,
          start_date = ${date.opt},
          end_date = ${date.opt},
          status = ${SkunkCodecs.tripStatus},
          updated_at = $timestamptz
      where user_id = $uuid
        and id = $uuid
      returning id, user_id, title::text, start_date, end_date, status, created_at, updated_at
    """.query(tripDecoder)

  val deleteQuery: Query[(UUID, UUID), UUID] =
    sql"""
      delete from trips
      where user_id = $uuid
        and id = $uuid
      returning id
    """.query(uuid)

  val existsForUserQuery: Query[(UUID, UUID), UUID] =
    sql"""
      select id
      from trips
      where user_id = $uuid
        and id = $uuid
    """.query(uuid)

  private def toListInput(userId: UserId, params: TripSearchParams): ListInput =
    (
      userId.value,
      params.status.isEmpty,
      params.status.getOrElse(TripStatus.Planned),
      params.fromDate.isEmpty,
      params.fromDate.getOrElse(LocalDate.of(1, 1, 1)),
      params.toDate.isEmpty,
      params.toDate.getOrElse(LocalDate.of(9999, 12, 31))
    )
