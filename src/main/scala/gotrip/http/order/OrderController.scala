package gotrip.http.order

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.userrole.Role
import gotrip.http.{HttpError, ValidationError}
import gotrip.http.auth.AuthSupport
import gotrip.service.order.{OrderService, OrderServiceError}
import sttp.tapir.server.ServerEndpoint

final class OrderController(service: OrderService[IO], authSupport: AuthSupport):

  val listTripOrders: ServerEndpoint[Any, IO] =
    OrderEndpoints.listTripOrders
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (tripId, serviceType, status, fromDate, toDate) =>
        service.listByTrip(user.userId, tripId, OrderSearchParams(serviceType, status, fromDate, toDate)).attempt.map {
          case Right(Right(orders)) => Right(orders)
          case Right(Left(error))   => Left(serviceError(error))
          case Left(error)          => Left(internalError(error))
        }
      }}

  val createOrder: ServerEndpoint[Any, IO] =
    OrderEndpoints.createOrder
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (tripId, order) =>
        OrderValidator.validate(order).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validOrder) =>
            service.create(user.userId, tripId, validOrder).attempt.map {
              case Right(Right(created)) => Right(created)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val getOrder: ServerEndpoint[Any, IO] =
    OrderEndpoints.getOrder
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => orderId =>
        service.findByUser(user.userId, orderId).attempt.map {
          case Right(Right(order)) => Right(order)
          case Right(Left(error))  => Left(serviceError(error))
          case Left(error)         => Left(internalError(error))
        }
      }

  val updateOrder: ServerEndpoint[Any, IO] =
    OrderEndpoints.updateOrder
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (orderId, order) =>
        OrderValidator.validate(order).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validOrder) =>
            service.update(user.userId, orderId, validOrder).attempt.map {
              case Right(Right(updated)) => Right(updated)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val deleteOrder: ServerEndpoint[Any, IO] =
    OrderEndpoints.deleteOrder
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => orderId =>
        service.delete(user.userId, orderId).attempt.map {
          case Right(Right(_))    => Right(())
          case Right(Left(error)) => Left(serviceError(error))
          case Left(error)        => Left(internalError(error))
        }
      }

  val updateOrderStatus: ServerEndpoint[Any, IO] =
    OrderEndpoints.updateOrderStatus
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (orderId, update) =>
        OrderValidator.validate(update).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validUpdate) =>
            service.updateStatus(user.userId, orderId, validUpdate).attempt.map {
              case Right(Right(updated)) => Right(updated)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val adminSimulateStatusChange: ServerEndpoint[Any, IO] =
    OrderEndpoints.adminSimulateStatusChange
      .serverSecurityLogic(token =>
        authSupport.authenticate(token).map(_.flatMap(user => authSupport.requireRole(user, Role.ADMIN)))
      )
      .serverLogic { _ => { case (orderId, update) =>
        OrderValidator.validate(update).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validUpdate) =>
            service.adminUpdateStatus(orderId, validUpdate).attempt.map {
              case Right(Right(updated)) => Right(updated)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val all: List[ServerEndpoint[Any, IO]] =
    List(
      listTripOrders,
      createOrder,
      getOrder,
      updateOrder,
      deleteOrder,
      updateOrderStatus,
      adminSimulateStatusChange
    )

  private def serviceError(error: OrderServiceError): OrderEndpoints.ErrorResponse =
    error match
      case OrderServiceError.TripNotFound(id) =>
        HttpError.NotFound(s"Trip with id ${id.value} was not found")
      case OrderServiceError.OrderNotFound(id) =>
        HttpError.NotFound(s"Order with id ${id.value} was not found")
      case OrderServiceError.ProviderNotFound(id) =>
        HttpError.NotFound(s"Provider with id ${id.value} was not found")
      case OrderServiceError.LocationNotFound(id) =>
        HttpError.NotFound(s"Location with id ${id.value} was not found")
      case OrderServiceError.InvalidDateTimeRange =>
        HttpError.Validation("Order start datetime must be before or equal to end datetime")

  private def internalError(error: Throwable): OrderEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
