package gotrip.http.review

import cats.effect.IO
import gotrip.domain.review._
import gotrip.http.HttpError
import gotrip.http.auth.AuthSupport
import gotrip.service.review.ReviewService
import sttp.tapir.server.ServerEndpoint
import ReviewCodecs.{ReviewCreateRequest, ReviewUpdateRequest}
import java.time.Instant
import java.util.UUID

final class ReviewController(service: ReviewService[IO], authSupport: AuthSupport):
  private val placeholderId = ReviewId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
  private val placeholderTime = Instant.EPOCH

  val listReviews: ServerEndpoint[Any, IO] =
    ReviewEndpoints.listReviews
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => { case (targetType, targetId) =>
      (targetType, targetId) match
        case (Some(tt), Some(tid)) =>
          service.findByTarget(tt, tid).attempt.map {
            case Right(list) => Right(list)
            case Left(error) => Left(HttpError.Internal(error.getMessage))
          }
        case _ =>
          IO.pure(Right(Nil))
    }}

  val createReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.createReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => request =>
      val userId = authUser.userId
      if (request.rating < 1 || request.rating > 5)
        IO.pure(Left(HttpError.Validation("Rating must be between 1 and 5")))
      else {
        val review = Review(
          id = placeholderId,
          userId = userId,
          targetType = request.targetType,
          targetId = request.targetId,
          rating = ReviewRating(request.rating),
          text = ReviewText(request.text),
          createdAt = placeholderTime,
          updatedAt = placeholderTime
        )
        service.create(review).attempt.map {
          case Right(created) => Right(created)
          case Left(error)    => Left(HttpError.Internal(error.getMessage))
        }
      }
    }

  val getReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.getReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => id =>
      service.findById(id).attempt.map {
        case Right(Some(r)) => Right(r)
        case Right(None)    => Left(HttpError.NotFound(s"Review ${id.value} not found"))
        case Left(error)    => Left(HttpError.Internal(error.getMessage))
      }
    }

  val updateReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.updateReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => { case (id, update) =>
      service.findById(id).flatMap {
        case Some(existing) =>
          val updated = existing.copy(
            rating = update.rating.map(ReviewRating.apply).getOrElse(existing.rating),
            text = update.text.map(s => ReviewText(Some(s))).getOrElse(existing.text)
          )
          service.update(updated).attempt.map {
            case Right(n) if n == 1 => Right(updated)
            case Right(n) if n == 0 => Left(HttpError.NotFound(s"Review ${id.value} not found"))
            case Right(n)           => Left(HttpError.Internal(s"Unexpected update count: $n"))
            case Left(error)        => Left(HttpError.Internal(error.getMessage))
          }
        case None =>
          IO.pure(Left(HttpError.NotFound(s"Review ${id.value} not found")))
      }
    }}

  val deleteReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.deleteReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => id =>
      service.delete(id).attempt.map {
        case Right(n) if n == 1 => Right(())
        case Right(n) if n == 0 => Left(HttpError.NotFound(s"Review ${id.value} not found"))
        case Right(n)           => Left(HttpError.Internal(s"Unexpected delete count: $n"))
        case Left(error)        => Left(HttpError.Internal(error.getMessage))
      }
    }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listReviews, createReview, getReview, updateReview, deleteReview)
