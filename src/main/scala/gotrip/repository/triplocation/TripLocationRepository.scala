package gotrip.repository.triplocation

import cats.effect.{Concurrent, Resource}
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.domain.user.UserId
import skunk.Session

trait TripLocationRepository[F[_]]:
  def listByTrip(tripId: TripId): F[List[TripLocation]]
  def findInTrip(tripId: TripId, tripLocationId: TripLocationId): F[Option[TripLocation]]
  def create(tripId: TripId, location: TripLocationCreate, visitOrder: VisitOrder): F[TripLocation]
  def update(tripId: TripId, tripLocationId: TripLocationId, location: TripLocationUpdate): F[Option[TripLocation]]
  def delete(tripId: TripId, tripLocationId: TripLocationId): F[Boolean]
  def tripExists(tripId: TripId): F[Boolean]
  def tripExistsForUser(userId: UserId, tripId: TripId): F[Boolean]
  def locationExists(locationId: LocationId): F[Boolean]
  def visitOrderExists(
    tripId: TripId,
    visitOrder: VisitOrder,
    excludeTripLocationId: Option[TripLocationId] = None
  ): F[Boolean]
  def nextVisitOrder(tripId: TripId): F[VisitOrder]

object TripLocationRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): TripLocationRepository[F] =
    PostgresTripLocationRepository.make(sessionPool)
