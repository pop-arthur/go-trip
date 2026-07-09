package gotrip.http.review

import cats.effect.IO
import gotrip.domain.review._
import gotrip.http.auth.AuthSupport
import gotrip.http.{HttpError}
import gotrip.service.review.ReviewService
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

final class ReviewController(
  service: ReviewService[IO],
  authSupport: AuthSupport
) {

  val listReviews: ServerEndpoint[Any, IO] =
    ReviewEndpoints.listReviews
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => { case (targetType, targetId) =>
        (targetType, targetId) match {
          case (Some(tt), Some(tid)) =>
            try {
              val tidUuid = UUID.fromString(tid)
              service.findByTarget(tt, ReviewTargetId(tidUuid)).attempt.map {
                case Right(list) => Right(list)
                case Left(error) => Left(HttpError.Internal(error.getMessage))
              }
            } catch {
              case _: IllegalArgumentException =>
                IO.pure(Left(HttpError.Validation("Invalid targetId format")))
            }
          case (Some(tt), None) =>
            service.findByTargetType(tt).attempt.map {
              case Right(list) => Right(list)
              case Left(error) => Left(HttpError.Internal(error.getMessage))
            }
          case _ =>
            service.findAll().attempt.map {
              case Right(list) => Right(list)
              case Left(error) => Left(HttpError.Internal(error.getMessage))
            }
        }
      } }

  val createReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.createReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { authUser => request =>
        val userId = authUser.userId
        try {
          val targetId = ReviewTargetId(UUID.fromString(request.targetId))
          if (request.rating < 1 || request.rating > 5)
            IO.pure(Left(HttpError.Validation("Rating must be between 1 and 5")))
          else {
            val now = java.time.Instant.now()
            val review = Review(
              id = ReviewId(UUID.randomUUID()),
              userId = userId,
              targetType = request.targetType,
              targetId = targetId,
              rating = ReviewRating(request.rating),
              text = ReviewText(request.text.map(_.trim).filter(_.nonEmpty)),
              createdAt = now,
              updatedAt = now
            )
            service.create(review).attempt.map {
              case Right(created) => Right(created)
              case Left(error)    => Left(HttpError.Internal(error.getMessage))
            }
          }
        } catch {
          case _: IllegalArgumentException =>
            IO.pure(Left(HttpError.Validation("Invalid targetId format, expected UUID")))
        }
      }

  val getReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.getReview.serverLogic { id =>
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
              text = update.text.map(t => ReviewText(Some(t))).getOrElse(existing.text),
              updatedAt = java.time.Instant.now()
            )
            service.update(updated).attempt.map {
              case Right(1)          => Right(updated)
              case Right(0)          => Left(HttpError.NotFound(s"Review ${id.value} not found"))
              case Right(otherCount) => Left(HttpError.Internal(s"Unexpected update count: $otherCount"))
              case Left(error)       => Left(HttpError.Internal(error.getMessage))
            }
          case None =>
            IO.pure(Left(HttpError.NotFound(s"Review ${id.value} not found")))
        }
      } }

  val deleteReview: ServerEndpoint[Any, IO] =
    ReviewEndpoints.deleteReview
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic { _ => id =>
        service.delete(id).attempt.map {
          case Right(1)          => Right(())
          case Right(0)          => Left(HttpError.NotFound(s"Review ${id.value} not found"))
          case Right(otherCount) => Left(HttpError.Internal(s"Unexpected delete count: $otherCount"))
          case Left(error)       => Left(HttpError.Internal(error.getMessage))
        }
      }

  val all: List[ServerEndpoint[Any, IO]] =
    List(listReviews, createReview, getReview, updateReview, deleteReview)
}