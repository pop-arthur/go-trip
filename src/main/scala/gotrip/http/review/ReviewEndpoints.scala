package gotrip.http.review

import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.http.{EndpointErrors, HttpError}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import ReviewCodecs.{ReviewCreateRequest, ReviewUpdateRequest, given}

object ReviewEndpoints:
  import ReviewCodecs.given

  type ErrorResponse = HttpError

  val listReviews
      : PublicEndpoint[(Option[ReviewTargetType], Option[ReviewTargetId]), ErrorResponse, List[Review], Any] =
    endpoint.get
      .in("reviews")
      .in(query[Option[ReviewTargetType]]("targetType"))
      .in(query[Option[ReviewTargetId]]("targetId"))
      .errorOut(EndpointErrors.internalOnly)
      .out(jsonBody[List[Review]])

  val createReview: PublicEndpoint[ReviewCreateRequest, ErrorResponse, Review, Any] =
    endpoint.post
      .in("reviews")
      .in(jsonBody[ReviewCreateRequest])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Review])

  val getReview: PublicEndpoint[ReviewId, ErrorResponse, Review, Any] =
    endpoint.get
      .in("reviews" / path[ReviewId]("reviewId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Review])

  val updateReview: PublicEndpoint[(ReviewId, ReviewUpdateRequest), ErrorResponse, Review, Any] =
    endpoint.patch
      .in("reviews" / path[ReviewId]("reviewId"))
      .in(jsonBody[ReviewUpdateRequest])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Review])

  val deleteReview: PublicEndpoint[ReviewId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .in("reviews" / path[ReviewId]("reviewId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))