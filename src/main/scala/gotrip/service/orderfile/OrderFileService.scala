package gotrip.service.orderfile

import cats.Monad
import cats.data.EitherT
import cats.effect.{Sync, Clock}
import cats.syntax.flatMap._
import cats.syntax.functor._
import gotrip.domain.order._
import gotrip.domain.user.UserId
import gotrip.repository.orderfile.OrderFileRepository
import gotrip.service.GeneratedData

import java.io.{File, FileOutputStream, InputStream}
import java.net.URL
import java.util.UUID

final class OrderFileService[F[_]: Sync: Clock: GeneratedData](repository: OrderFileRepository[F]) {

  import OrderFileServiceError._

  private val uploadDir = "uploads"

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
      localPath <- EitherT.liftF(downloadFile(file.file_url.value))
      materialized <- EitherT.liftF(materializeFile(orderId, file.copy(file_url = OrderFileUrl(localPath))))
      created <- EitherT.fromOptionF(repository.create(userId, materialized), OrderNotFound(orderId))
    } yield created).value

  def delete(
    userId: UserId,
    orderId: OrderId,
    fileId: OrderFileId
  ): F[Either[OrderFileServiceError, Unit]] =
    (for {
      _ <- ensureOrderExists(userId, orderId)
      fileOpt <- EitherT.liftF(repository.findByOrder(userId, orderId, fileId))
      file <- EitherT.fromOption(fileOpt, OrderFileNotFound(fileId))
      _ <- EitherT.liftF(deleteLocalFile(file.file_url.value))
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

  private def downloadFile(urlString: String): F[String] =
    Sync[F].blocking {
      val dir = new File(uploadDir)
      if (!dir.exists()) dir.mkdirs()

      val url = new URL(urlString)
      val connection = url.openConnection()
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(10000)

      val extension = Option(connection.getContentType)
        .map(ct => ct.split('/').last)
        .filter(ext => ext.matches("^[a-zA-Z0-9]+$"))
        .map(ext => s".$ext")
        .getOrElse {
          val name = urlString.split('/').last
          val ext = if (name.contains('.')) name.substring(name.lastIndexOf('.')) else ".bin"
          ext
        }

      val fileName = s"${UUID.randomUUID()}$extension"
      val filePath = s"$uploadDir/$fileName"
      val outputFile = new File(filePath)

      val inputStream: InputStream = connection.getInputStream
      val outputStream = new FileOutputStream(outputFile)
      try {
        inputStream.transferTo(outputStream)
      } finally {
        inputStream.close()
        outputStream.close()
      }

      filePath
    }

  private def deleteLocalFile(path: String): F[Unit] =
    Sync[F].blocking {
      val file = new File(path)
      if (file.exists()) file.delete()
    }

  private def materializeFile(orderId: OrderId, create: OrderFileCreate): F[OrderFile] =
    for {
      id <- GeneratedData[F].newId()
      now <- GeneratedData[F].now()
    } yield OrderFile(
      id = OrderFileId(id),
      order_id = orderId,
      file_url = create.file_url,
      file_type = create.file_type,
      parsed_data = create.parsed_data,
      uploaded_at = now
    )
}

enum OrderFileServiceError:
  case OrderNotFound(id: OrderId)
  case OrderFileNotFound(id: OrderFileId)