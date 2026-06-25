package gotrip.repository.order

import cats.effect.{Concurrent, Resource}
import gotrip.domain.location.LocationId
import gotrip.domain.order.*
import gotrip.domain.provider.ProviderId
import gotrip.domain.trip.TripId
import gotrip.domain.user.UserId
import skunk.Session

trait OrderRepository[F[_]]:
  def listByTrip(userId: UserId, tripId: TripId, params: OrderSearchParams): F[List[Order]]
  def findByUser(userId: UserId, orderId: OrderId): F[Option[Order]]
  def create(userId: UserId, tripId: TripId, order: OrderCreate): F[Order]
  def update(userId: UserId, orderId: OrderId, order: OrderUpdate): F[Option[Order]]
  def delete(userId: UserId, orderId: OrderId): F[Boolean]
  def updateStatus(userId: UserId, orderId: OrderId, update: OrderStatusUpdate): F[Option[Order]]
  def insertStatusEvent(event: OrderStatusEvent): F[OrderStatusEvent]
  def tripExistsForUser(userId: UserId, tripId: TripId): F[Boolean]
  def providerExists(providerId: ProviderId): F[Boolean]
  def locationExists(locationId: LocationId): F[Boolean]

object OrderRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): OrderRepository[F] =
    PostgresOrderRepository.make(sessionPool)
