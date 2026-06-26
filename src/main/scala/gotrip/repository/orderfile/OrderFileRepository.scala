package gotrip.repository.orderfile

import cats.effect.{Concurrent, Resource}
import gotrip.domain.order.*
import gotrip.domain.user.UserId
import skunk.Session

trait OrderFileRepository[F[_]]:
  def listByOrder(userId: UserId, orderId: OrderId): F[List[OrderFile]]
  def findByOrder(userId: UserId, orderId: OrderId, fileId: OrderFileId): F[Option[OrderFile]]
  def create(userId: UserId, orderId: OrderId, file: OrderFileCreate): F[Option[OrderFile]]
  def delete(userId: UserId, orderId: OrderId, fileId: OrderFileId): F[Boolean]
  def orderExistsForUser(userId: UserId, orderId: OrderId): F[Boolean]

object OrderFileRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): OrderFileRepository[F] =
    PostgresOrderFileRepository.make(sessionPool)
