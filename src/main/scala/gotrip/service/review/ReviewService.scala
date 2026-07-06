package gotrip.service.review

import cats.Monad
import cats.syntax.all._
import gotrip.domain.review.{Review, ReviewId, ReviewTargetType, ReviewTargetId}
import gotrip.domain.user.UserId
import gotrip.repository.review.ReviewRepository
import gotrip.service.achievement.{AchievementEngine, AchievementEvent}

final class ReviewService[F[_]: Monad](
  repo: ReviewRepository[F],
  achievementEngine: AchievementEngine[F]
):

  def create(review: Review): F[Review] =
    repo.create(review).flatMap { created =>
      achievementEngine.checkAndUnlock(created.userId, AchievementEvent.ReviewCreated(created)).map(_ => created)
    }

  def findById(id: ReviewId): F[Option[Review]] = repo.findById(id)

  def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
    repo.findByTarget(targetType, targetId)

  def findByUser(userId: UserId): F[List[Review]] =
    repo.findByUserId(userId)

  def update(review: Review): F[Int] = repo.update(review)

  def delete(id: ReviewId): F[Int] = repo.delete(id)

  def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] =
    repo.averageRating(targetType, targetId)