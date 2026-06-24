package gotrip.http.triplocation

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.domain.trip.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.service.triplocation.{TripLocationService, TripLocationServiceError}
import sttp.tapir.server.ServerEndpoint

final class TripLocationController(service: TripLocationService[IO]):

  val listTripLocations: ServerEndpoint[Any, IO] =
    TripLocationEndpoints.listTripLocations.serverLogic { tripId =>
      service.listByTrip(tripId).attempt.map {
        case Right(Right(locations)) =>
          Right(locations)

        case Right(Left(error)) =>
          Left(serviceError(error))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val addTripLocation: ServerEndpoint[Any, IO] =
    TripLocationEndpoints.addTripLocation.serverLogic { case (tripId, location) =>
      TripLocationValidator.validate(location).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validLocation) =>
          service.create(tripId, validLocation).attempt.map {
            case Right(Right(created)) =>
              Right(created)

            case Right(Left(error)) =>
              Left(serviceError(error))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val updateTripLocation: ServerEndpoint[Any, IO] =
    TripLocationEndpoints.updateTripLocation.serverLogic { case (tripId, tripLocationId, location) =>
      TripLocationValidator.validate(location).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validLocation) =>
          service.update(tripId, tripLocationId, validLocation).attempt.map {
            case Right(Right(updated)) =>
              Right(updated)

            case Right(Left(error)) =>
              Left(serviceError(error))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val deleteTripLocation: ServerEndpoint[Any, IO] =
    TripLocationEndpoints.deleteTripLocation.serverLogic { case (tripId, tripLocationId) =>
      service.delete(tripId, tripLocationId).attempt.map {
        case Right(Right(_)) =>
          Right(())

        case Right(Left(error)) =>
          Left(serviceError(error))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listTripLocations, addTripLocation, updateTripLocation, deleteTripLocation)

  private def serviceError(error: TripLocationServiceError): TripLocationEndpoints.ErrorResponse =
    error match
      case TripLocationServiceError.TripNotFound(id) =>
        notFound(s"Trip with id ${id.value} was not found")

      case TripLocationServiceError.LocationNotFound(id) =>
        notFound(locationNotFoundMessage(id))

      case TripLocationServiceError.TripLocationNotFound(id) =>
        notFound(s"Trip location with id ${id.value} was not found")

      case TripLocationServiceError.DuplicateVisitOrder(visitOrder) =>
        HttpError.Validation(s"Visit order ${visitOrder.value} is already used in this trip")

      case TripLocationServiceError.InvalidDateRange =>
        HttpError.Validation("Arrival date must be before or equal to departure date")

  private def locationNotFoundMessage(id: LocationId): String =
    s"Location with id ${id.value} was not found"

  private def notFound(message: String): TripLocationEndpoints.ErrorResponse =
    HttpError.NotFound(message)

  private def internalError(error: Throwable): TripLocationEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
