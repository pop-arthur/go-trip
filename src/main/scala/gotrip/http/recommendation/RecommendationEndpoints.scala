package gotrip.http.recommendation

import gotrip.domain.order.OrderId
import gotrip.domain.recommendation.Recommendation
import gotrip.domain.trip.TripId
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import gotrip.http.order.OrderCodecs.given
import gotrip.http.recommendation.RecommendationCodecs.given
import gotrip.http.trip.TripCodecs.given
import sttp.tapir.*
import sttp.tapir.json.circe.*

object RecommendationEndpoints:
  type ErrorResponse = HttpError

  val getTripRecommendations: Endpoint[String, TripId, ErrorResponse, List[Recommendation], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "recommendations")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[Recommendation]])

  val getOrderRecommendations: Endpoint[String, OrderId, ErrorResponse, List[Recommendation], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "recommendations")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[Recommendation]])
