package gotrip.http.trip

import cats.effect.IO
import gotrip.domain.trip.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.http.auth.AuthSupport
import gotrip.service.trip.{TripService, TripServiceError}
import sttp.tapir.server.ServerEndpoint

final class TripController(service: TripService[IO], authSupport: AuthSupport):

  val listTrips: ServerEndpoint[Any, IO] =
    TripEndpoints.listTrips
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (status, fromDate, toDate) =>
        service.listByUser(user.userId, TripSearchParams(status, fromDate, toDate)).attempt.map {
          case Right(trips) => Right(trips)
          case Left(error)  => Left(internalError(error))
        }
      }}

  val createTrip: ServerEndpoint[Any, IO] =
    TripEndpoints.createTrip
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => trip =>
        TripValidator.validate(trip).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validTrip) =>
            service.create(user.userId, validTrip).attempt.map {
              case Right(Right(created)) => Right(created)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }

  val getTrip: ServerEndpoint[Any, IO] =
    TripEndpoints.getTrip
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => tripId =>
        service.findByUser(user.userId, tripId).attempt.map {
          case Right(Right(trip))  => Right(trip)
          case Right(Left(error))  => Left(serviceError(error))
          case Left(error)         => Left(internalError(error))
        }
      }

  val updateTrip: ServerEndpoint[Any, IO] =
    TripEndpoints.updateTrip
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => { case (tripId, trip) =>
        TripValidator.validate(trip).toEither match
          case Left(errors) =>
            IO.pure(Left(ValidationError.toHttpError(errors)))
          case Right(validTrip) =>
            service.update(user.userId, tripId, validTrip).attempt.map {
              case Right(Right(updated)) => Right(updated)
              case Right(Left(error))    => Left(serviceError(error))
              case Left(error)           => Left(internalError(error))
            }
      }}

  val deleteTrip: ServerEndpoint[Any, IO] =
    TripEndpoints.deleteTrip
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => tripId =>
        service.delete(user.userId, tripId).attempt.map {
          case Right(Right(_))     => Right(())
          case Right(Left(error))  => Left(serviceError(error))
          case Left(error)         => Left(internalError(error))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listTrips, createTrip, getTrip, updateTrip, deleteTrip)

  private def serviceError(error: TripServiceError): TripEndpoints.ErrorResponse =
    error match
      case TripServiceError.TripNotFound(id) =>
        HttpError.NotFound(s"Trip with id ${id.value} was not found")
      case TripServiceError.InvalidDateRange =>
        HttpError.Validation("Trip start date must be before or equal to end date")

  private def internalError(error: Throwable): TripEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
