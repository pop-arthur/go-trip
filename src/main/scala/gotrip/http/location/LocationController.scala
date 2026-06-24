package gotrip.http.location

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.http.{HttpError, ValidationError}
import gotrip.http.auth.AuthSupport
import gotrip.service.location.LocationService
import sttp.tapir.server.ServerEndpoint

final class LocationController(service: LocationService[IO], authSupport: AuthSupport):

  val listLocations: ServerEndpoint[Any, IO] =
    LocationEndpoints.listLocations
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => { case (locationType, country, city, query) =>
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
    }}

  val getLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.getLocation
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => id =>
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
    LocationEndpoints.createLocation
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => location =>
      LocationValidator.validate(location).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validLocation) =>
          service.create(validLocation).attempt.map {
            case Right(created) =>
              Right(created)

            case Left(error) =>
              Left(internalError(error))
          }
    }

  val updateLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.updateLocation
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => { case (id, location) =>
      LocationValidator.validate(location).toEither match
        case Left(errors) =>
          IO.pure(Left(ValidationError.toHttpError(errors)))
        case Right(validLocation) =>
          service.update(id, validLocation).attempt.map {
            case Right(Some(updated)) =>
              Right(updated)

            case Right(None) =>
              Left(notFound(id))

            case Left(error) =>
              Left(internalError(error))
          }
    }}

  val deleteLocation: ServerEndpoint[Any, IO] =
    LocationEndpoints.deleteLocation
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => id =>
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
    HttpError.NotFound(s"Location with id ${id.value} was not found")

  private def internalError(error: Throwable): LocationEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
