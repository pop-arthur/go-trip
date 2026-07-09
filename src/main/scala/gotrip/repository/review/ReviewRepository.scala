package gotrip.repository.review

import cats.effect.{Concurrent, Resource}
import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId
import skunk.Session

trait ReviewRepository[F[_]]:
  def create(review: Review): F[Review]
  def findById(id: ReviewId): F[Option[Review]]
  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]]
  def findByTargetType(targetType: ReviewTargetType): F[List[Review]]
  def findByUserId(userId: UserId): F[List[Review]]
  def findAll(): F[List[Review]]
  def update(review: Review): F[Int]
  def delete(id: ReviewId): F[Int]
  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]]
  
  def countByUser(userId: UserId): F[Int]

object ReviewRepository:

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): ReviewRepository[F] =
    PostgresReviewRepository.make(sessionPool)