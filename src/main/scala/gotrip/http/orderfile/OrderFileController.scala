package gotrip.http.orderfile

import cats.effect.IO
import gotrip.domain.order.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.http.auth.AuthSupport
import gotrip.service.orderfile.{OrderFileService, OrderFileServiceError}
import sttp.tapir.server.ServerEndpoint

final class OrderFileController(service: OrderFileService[IO], authSupport: AuthSupport):

  val listOrderFiles: ServerEndpoint[Any, IO] =
    OrderFileEndpoints.listOrderFiles
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => orderId =>
        service.listByOrder(user.userId, orderId).attempt.map {
          case Right(Right(files)) => Right(files)
          case Right(Left(error))  => Left(serviceError(error))
          case Left(error)         => Left(internalError(error))
        }
      }

  val createOrderFile: ServerEndpoint[Any, IO] =
    OrderFileEndpoints.createOrderFile
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (orderId, file) =>
        OrderFileValidator.validate(file).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validFile) =>
            service.create(user.userId, orderId, validFile).attempt.map {
              case Right(Right(created)) => Right(created)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val getOrderFile: ServerEndpoint[Any, IO] =
    OrderFileEndpoints.getOrderFile
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (orderId, fileId) =>
        service.findByOrder(user.userId, orderId, fileId).attempt.map {
          case Right(Right(file)) => Right(file)
          case Right(Left(error)) => Left(serviceError(error))
          case Left(error)        => Left(internalError(error))
        }
      }}

  val deleteOrderFile: ServerEndpoint[Any, IO] =
    OrderFileEndpoints.deleteOrderFile
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (orderId, fileId) =>
        service.delete(user.userId, orderId, fileId).attempt.map {
          case Right(Right(_))    => Right(())
          case Right(Left(error)) => Left(serviceError(error))
          case Left(error)        => Left(internalError(error))
        }
      }}

  val all: List[ServerEndpoint[Any, IO]] =
    List(listOrderFiles, createOrderFile, getOrderFile, deleteOrderFile)

  private def serviceError(error: OrderFileServiceError): OrderFileEndpoints.ErrorResponse =
    error match
      case OrderFileServiceError.OrderNotFound(id) =>
        HttpError.NotFound(s"Order with id ${id.value} was not found")
      case OrderFileServiceError.OrderFileNotFound(id) =>
        HttpError.NotFound(s"Order file with id ${id.value} was not found")

  private def internalError(error: Throwable): OrderFileEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
