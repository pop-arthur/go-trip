package gotrip.repository.review

import cats.Applicative
import cats.effect.Concurrent
import skunk.Session
import cats.effect.Resource
import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId

trait ReviewRepository[F[_]]:
  def create(review: Review): F[Review]
  def findById(id: ReviewId): F[Option[Review]]
  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]]
  def findByUserId(userId: UserId): F[List[Review]]
  def update(review: Review): F[Int]
  def delete(id: ReviewId): F[Int]
  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]]

object ReviewRepository:
  def makeInMemory[F[_]: Applicative]: F[ReviewRepository[F]] =
    InMemoryReviewRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): ReviewRepository[F] =
    PostgresReviewRepository.make(sessionPool)