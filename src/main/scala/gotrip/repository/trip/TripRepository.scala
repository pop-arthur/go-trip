package gotrip.repository.trip

import cats.effect.{Concurrent, Resource}
import gotrip.domain.trip.*
import gotrip.domain.user.UserId
import skunk.Session

trait TripRepository[F[_]]:
  def listByUser(userId: UserId, params: TripSearchParams): F[List[Trip]]
  def findByUser(userId: UserId, tripId: TripId): F[Option[Trip]]
  def create(trip: Trip): F[Trip]
  def update(trip: Trip): F[Option[Trip]]
  def delete(userId: UserId, tripId: TripId): F[Boolean]
  def existsForUser(userId: UserId, tripId: TripId): F[Boolean]

object TripRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): TripRepository[F] =
    PostgresTripRepository.make(sessionPool)
