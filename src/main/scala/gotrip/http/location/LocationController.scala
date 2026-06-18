package gotrip.http.location

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.service.location.LocationService
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

final class LocationController(service: LocationService[IO]):

  val listLocations: ServerEndpoint[Any, IO] =
    LocationEndpoints.listLocations.serverLogic { case (locationType, country, city, query) =>
      val params = LocationSearchParams(
        `type` = locationType,
        country = country,
        city = city,
        query = query
      )

      service.search(params).attempt.map {
        case Right(locations) =>
          Right(locations)

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val getLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.getLocation.serverLogic { id =>
      service.findById(id).attempt.map {
        case Right(Some(location)) =>
          Right(location)

        case Right(None) =>
          Left(notFound(id))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val createLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.createLocation.serverLogic { location =>
      LocationValidator.validate(location) match
        case Left(error) =>
          IO.pure(Left(validationError(error)))
        case Right(validLocation) =>
          service.create(validLocation).attempt.map {
            case Right(created) =>
              Right(created)

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val updateLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.updateLocation.serverLogic { case (id, location) =>
      LocationValidator.validate(location) match
        case Left(error) =>
          IO.pure(Left(validationError(error)))
        case Right(validLocation) =>
          service.update(id, validLocation).attempt.map {
            case Right(Some(updated)) =>
              Right(updated)

            case Right(None) =>
              Left(notFound(id))

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val deleteLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.deleteLocation.serverLogic { id =>
      service.delete(id).attempt.map {
        case Right(true) =>
          Right(())

        case Right(false) =>
          Left(notFound(id))

        case Left(error) =>
          Left(internalError(error))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listLocations, getLocation, createLocation, updateLocation, deleteLocation)

  private def notFound(id: LocationId): LocationEndpoints.ErrorResponse =
    StatusCode.NotFound -> ApiError("NOT_FOUND", s"Location with id ${id.value} was not found")

  private def validationError(error: ApiError): LocationEndpoints.ErrorResponse =
    StatusCode.UnprocessableEntity -> error

  private def internalError(error: Throwable): LocationEndpoints.ErrorResponse =
    StatusCode.InternalServerError -> ApiError("INTERNAL_ERROR", error.getMessage)
