package gotrip.service.review

import cats.effect.{Clock, Sync}
import cats.syntax.all._
import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId
import gotrip.repository.review.ReviewRepository
import gotrip.service.GeneratedData
import gotrip.service.achievement.{AchievementEngine, AchievementEvent}

final class ReviewService[F[_]: Sync: Clock: GeneratedData](
  repo: ReviewRepository[F],
  achievementEngine: AchievementEngine[F]
) {

  def create(review: Review): F[Review] =
    for {
      id <- GeneratedData[F].newId()
      now <- GeneratedData[F].now()
      created <- repo.create(review.copy(id = ReviewId(id), createdAt = now, updatedAt = now))
      _ <- achievementEngine.checkAndUnlock(review.userId, AchievementEvent.ReviewCreated(created))
    } yield created

  def findById(id: ReviewId): F[Option[Review]] = repo.findById(id)

  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
    repo.findByTarget(targetType, targetId)

  def findByUser(userId: UserId): F[List[Review]] =
    repo.findByUserId(userId)

  def update(review: Review): F[Int] =
    GeneratedData[F].now().flatMap(now => repo.update(review.copy(updatedAt = now)))

  def delete(id: ReviewId): F[Int] = repo.delete(id)

  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] =
    repo.averageRating(targetType, targetId)
}