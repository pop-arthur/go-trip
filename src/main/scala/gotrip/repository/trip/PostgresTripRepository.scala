package gotrip.repository.trip

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.SkunkCodecs
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.{Instant, LocalDate}
import java.util.UUID

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

  override def create(userId: UserId, trip: TripCreate): F[Trip] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.createQuery).flatMap { query =>
        val newId = UUID.randomUUID()
        query.unique(
          (
            newId,
            userId.value,
            trip.title.value,
            trip.start_date.value,
            trip.end_date.value,
            trip.status.getOrElse(TripStatus.Planned)
          )
        )
      }
    }

  override def update(userId: UserId, tripId: TripId, trip: TripUpdate): F[Option[Trip]] =
    sessionPool.use { session =>
      PostgresTripRepository.updateFragment(userId, tripId, trip) match
        case Some(fragment) =>
          session.prepare(fragment.fragment.query(PostgresTripRepository.tripDecoder)).flatMap { query =>
            query.option(fragment.argument)
          }
        case None =>
          session.prepare(PostgresTripRepository.findByUserQuery).flatMap { query =>
            query.option((userId.value, tripId.value))
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

  override def countByUser(userId: UserId): F[Int] =
    sessionPool.use { session =>
      session.prepare(PostgresTripRepository.countByUserQuery).flatMap { q =>
        q.unique(userId.value)
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

  val createQuery: Query[(UUID, UUID, String, Option[LocalDate], Option[LocalDate], TripStatus), Trip] =
    sql"""
      insert into trips (id, user_id, title, start_date, end_date, status)
      values ($uuid, $uuid, $text, ${date.opt}, ${date.opt}, ${SkunkCodecs.tripStatus})
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

  val countByUserQuery: Query[UUID, Int] =
    sql"SELECT COUNT(*) FROM trips WHERE user_id = $uuid".query(int4)

  private def updateFragment(
    userId: UserId,
    tripId: TripId,
    trip: TripUpdate
  ): Option[AppliedFragment] =
    val fields =
      List(
        trip.title.map(value => sql"title = $text"(value.value)),
        trip.start_date.map(value => sql"start_date = ${date.opt}"(value.value)),
        trip.end_date.map(value => sql"end_date = ${date.opt}"(value.value)),
        trip.status.map(value => sql"status = ${SkunkCodecs.tripStatus}"(value))
      ).flatten

    fields.headOption.map { head =>
      val sets = combineApplied(head :: fields.tail)
      AppliedFragment(sql"update trips set ${sets.fragment}, updated_at = now()", sets.argument) |+|
        sql"""
          where user_id = $uuid
            and id = $uuid
          returning id, user_id, title::text, start_date, end_date, status, created_at, updated_at
        """((userId.value, tripId.value))
    }

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

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }