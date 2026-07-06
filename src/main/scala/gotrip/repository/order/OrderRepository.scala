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
  def listExternalByUser(userId: UserId): F[List[Order]]
  def findById(orderId: OrderId): F[Option[Order]]
  def findByUser(userId: UserId, orderId: OrderId): F[Option[Order]]
  def create(order: Order): F[Order]
  def update(order: Order): F[Option[Order]]
  def delete(userId: UserId, orderId: OrderId): F[Boolean]
  def updateStatus(order: Order): F[Option[Order]]
  def insertStatusEvent(event: OrderStatusEvent): F[OrderStatusEvent]
  def tripExistsForUser(userId: UserId, tripId: TripId): F[Boolean]
  def providerExists(providerId: ProviderId): F[Boolean]
  def locationExists(locationId: LocationId): F[Boolean]

object OrderRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): OrderRepository[F] =
    PostgresOrderRepository.make(sessionPool)
