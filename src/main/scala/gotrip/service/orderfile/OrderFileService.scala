package gotrip.service.orderfile

import cats.Monad
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.order.*
import gotrip.domain.user.UserId
import gotrip.repository.orderfile.OrderFileRepository
import gotrip.service.GeneratedData

final class OrderFileService[F[_]: Sync: Clock: GeneratedData](repository: OrderFileRepository[F]):

  import OrderFileServiceError.*

  def listByOrder(userId: UserId, orderId: OrderId): F[Either[OrderFileServiceError, List[OrderFile]]] =
    (for {
      _ <- ensureOrderExists(userId, orderId)
      files <- EitherT.liftF(repository.listByOrder(userId, orderId))
    } yield files).value

  def findByOrder(
    userId: UserId,
    orderId: OrderId,
    fileId: OrderFileId
  ): F[Either[OrderFileServiceError, OrderFile]] =
    EitherT.fromOptionF(repository.findByOrder(userId, orderId, fileId), OrderFileNotFound(fileId)).value

  def create(
    userId: UserId,
    orderId: OrderId,
    file: OrderFileCreate
  ): F[Either[OrderFileServiceError, OrderFile]] =
    (for {
      _ <- ensureOrderExists(userId, orderId)
      materialized <- EitherT.liftF(materializeFile(orderId, file))
      created <- EitherT.fromOptionF(repository.create(userId, materialized), OrderNotFound(orderId))
    } yield created).value

  def delete(
    userId: UserId,
    orderId: OrderId,
    fileId: OrderFileId
  ): F[Either[OrderFileServiceError, Unit]] =
    (for {
      _ <- ensureOrderExists(userId, orderId)
      deleted <- EitherT.liftF(repository.delete(userId, orderId, fileId))
      _ <- if deleted then EitherT.rightT[F, OrderFileServiceError](())
           else EitherT.leftT[F, Unit](OrderFileNotFound(fileId))
    } yield ()).value

  private def ensureOrderExists(userId: UserId, orderId: OrderId): EitherT[F, OrderFileServiceError, Unit] =
    EitherT {
      repository.orderExistsForUser(userId, orderId).map { exists =>
        Either.cond(exists, (), OrderNotFound(orderId))
      }
    }

  private def materializeFile(orderId: OrderId, create: OrderFileCreate): F[OrderFile] =
    for
      id <- GeneratedData[F].newId()
      now <- GeneratedData[F].now()
    yield OrderFile(
      id = OrderFileId(id),
      order_id = orderId,
      file_url = create.file_url,
      file_type = create.file_type,
      parsed_data = create.parsed_data,
      uploaded_at = now
    )

enum OrderFileServiceError:
  case OrderNotFound(id: OrderId)
  case OrderFileNotFound(id: OrderFileId)
