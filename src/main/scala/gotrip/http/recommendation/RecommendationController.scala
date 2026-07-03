package gotrip.http.recommendation

import cats.effect.IO
import gotrip.domain.order.*
import gotrip.domain.trip.*
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.recommendation.{RecommendationService, RecommendationServiceError}
import sttp.tapir.server.ServerEndpoint

final class RecommendationController(service: RecommendationService[IO], authSupport: AuthSupport):

  val getTripRecommendations: ServerEndpoint[Any, IO] =
    RecommendationEndpoints.getTripRecommendations
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => tripId =>
        service.forTrip(user.userId, tripId).attempt.map {
          case Right(Right(recommendations)) => Right(recommendations)
          case Right(Left(error))            => Left(serviceError(error))
          case Left(error)                   => Left(internalError(error))
        }
      }

  val getOrderRecommendations: ServerEndpoint[Any, IO] =
    RecommendationEndpoints.getOrderRecommendations
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { user => orderId =>
        service.forOrder(user.userId, orderId).attempt.map {
          case Right(Right(recommendations)) => Right(recommendations)
          case Right(Left(error))            => Left(serviceError(error))
          case Left(error)                   => Left(internalError(error))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(getTripRecommendations, getOrderRecommendations)

  private def serviceError(error: RecommendationServiceError): RecommendationEndpoints.ErrorResponse =
    error match
      case RecommendationServiceError.TripNotFound(id) =>
        HttpError.NotFound(s"Trip with id ${id.value} was not found")
      case RecommendationServiceError.OrderNotFound(id) =>
        HttpError.NotFound(s"Order with id ${id.value} was not found")

  private def internalError(error: Throwable): RecommendationEndpoints.ErrorResponse =
    HttpError.Internal(error.getMessage)
