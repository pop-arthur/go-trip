package gotrip.service.review

import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import gotrip.domain.review.{Review, ReviewId, ReviewRatingSummary, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId
import gotrip.repository.review.ReviewRepository
import gotrip.service.GeneratedData
import gotrip.service.achievement.{AchievementEngine, AchievementEvent}

trait ReviewService[F[_]] {
  def create(review: Review): F[Review]
  def findById(id: ReviewId): F[Option[Review]]
  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]]
  def findByTargetType(targetType: ReviewTargetType): F[List[Review]]
  def findByUser(userId: UserId): F[List[Review]]
  def findAll(): F[List[Review]]
  def update(review: Review): F[Int]
  def delete(id: ReviewId): F[Int]
  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]]
  def getRatingSummary(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[ReviewRatingSummary]]
}

object ReviewService {
  def make[F[_]: Sync: Clock: GeneratedData](
    repo: ReviewRepository[F],
    achievementEngine: AchievementEngine[F]
  ): ReviewService[F] =
    new ReviewService[F] {
      override def create(review: Review): F[Review] =
        for {
          id <- GeneratedData[F].newId()
          now <- GeneratedData[F].now()
          created <- repo.create(review.copy(id = ReviewId(id), createdAt = now, updatedAt = now))
          _ <- achievementEngine.checkAndUnlock(review.userId, AchievementEvent.ReviewCreated(created))
        } yield created

      override def findById(id: ReviewId): F[Option[Review]] = repo.findById(id)
      override def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
        repo.findByTarget(targetType, targetId)
      override def findByTargetType(targetType: ReviewTargetType): F[List[Review]] =
        repo.findByTargetType(targetType)
      override def findByUser(userId: UserId): F[List[Review]] =
        repo.findByUserId(userId)
      override def findAll(): F[List[Review]] = repo.findAll()
      override def update(review: Review): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.update(review.copy(updatedAt = now)))
      override def delete(id: ReviewId): F[Int] = repo.delete(id)
      override def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] =
        repo.averageRating(targetType, targetId)
      override def getRatingSummary(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[ReviewRatingSummary]] =
        repo.getRatingSummary(targetType, targetId)
    }
}