package gotrip.http.review

import gotrip.domain.review._
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._

import ReviewCodecs.{ReviewCreateRequest, ReviewUpdateRequest, given}

object ReviewEndpoints {
  type ErrorResponse = HttpError

  val listReviews: Endpoint[String, (Option[ReviewTargetType], Option[String]), ErrorResponse, List[Review], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("reviews")
      .in(query[Option[ReviewTargetType]]("targetType"))
      .in(query[Option[String]]("targetId"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Review]])

  val createReview: Endpoint[String, ReviewCreateRequest, ErrorResponse, Review, Any] =
    endpoint.post
      .securityIn(AuthEndpoints.bearer)
      .in("reviews")
      .in(jsonBody[ReviewCreateRequest])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Review])

  val getReview: PublicEndpoint[ReviewId, ErrorResponse, Review, Any] =
    endpoint.get
      .in("reviews" / path[ReviewId]("reviewId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Review])

  val updateReview: Endpoint[String, (ReviewId, ReviewUpdateRequest), ErrorResponse, Review, Any] =
    endpoint.patch
      .securityIn(AuthEndpoints.bearer)
      .in("reviews" / path[ReviewId]("reviewId"))
      .in(jsonBody[ReviewUpdateRequest])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Review])

  val deleteReview: Endpoint[String, ReviewId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .securityIn(AuthEndpoints.bearer)
      .in("reviews" / path[ReviewId]("reviewId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
}