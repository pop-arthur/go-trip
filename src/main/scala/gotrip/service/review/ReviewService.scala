package gotrip.service.review

import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId
import gotrip.repository.review.ReviewRepository

final class ReviewService[F[_]](
  repo: ReviewRepository[F]
):

  def create(review: Review): F[Review] = repo.create(review)

  def findById(id: ReviewId): F[Option[Review]] = repo.findById(id)

  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
    repo.findByTarget(targetType, targetId)

  def findByUser(userId: UserId): F[List[Review]] =
    repo.findByUserId(userId)

  def update(review: Review): F[Int] = repo.update(review)
  def delete(id: ReviewId): F[Int] = repo.delete(id)

  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] =
    repo.averageRating(targetType, targetId)