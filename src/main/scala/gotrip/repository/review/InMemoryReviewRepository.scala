package gotrip.repository.review

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.review._
import gotrip.domain.user.UserId
import java.time.Instant
import java.util.UUID
import scala.collection.mutable

object InMemoryReviewRepository {
  def make[F[_]: Applicative]: F[ReviewRepository[F]] = {
    val store = mutable.Map.empty[ReviewId, Review]
    def newId(): ReviewId = ReviewId(UUID.randomUUID())

    new ReviewRepository[F] {
      override def create(review: Review): F[Review] = {
        val now = Instant.now()
        val newReview = review.copy(
          id = newId(),
          createdAt = now,
          updatedAt = now
        )
        store += (newReview.id -> newReview)
        newReview.pure[F]
      }

      override def findById(id: ReviewId): F[Option[Review]] = store.get(id).pure[F]

      override def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
        store.values.filter(r => r.targetType == targetType && r.targetId == targetId).toList.pure[F]

      override def findByTargetType(targetType: ReviewTargetType): F[List[Review]] =
        store.values.filter(_.targetType == targetType).toList.pure[F]

      override def findByUserId(userId: UserId): F[List[Review]] =
        store.values.filter(_.userId == userId).toList.pure[F]

      override def findAll(): F[List[Review]] =
        store.values.toList.pure[F]

      override def update(review: Review): F[Int] = {
        store.get(review.id) match {
          case Some(_) =>
            val updated = review.copy(updatedAt = Instant.now())
            store += (updated.id -> updated)
            1.pure[F]
          case None => 0.pure[F]
        }
      }

      override def delete(id: ReviewId): F[Int] =
        store.remove(id).map(_ => 1).getOrElse(0).pure[F]

      override def averageRating(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[Double]] = {
        val ratings = store.values.filter(r => r.targetType == targetType && r.targetId == targetId).map(_.rating.value).toList
        ratings match {
          case Nil => None.pure[F]
          case list => Some(list.map(_.toDouble).sum / list.size).pure[F]
        }
      }

      override def countByUser(userId: UserId): F[Int] =
        store.values.count(_.userId == userId).pure[F]

      override def getRatingSummary(targetType: ReviewTargetType, targetId: ReviewTargetId): F[Option[ReviewRatingSummary]] = {
        val targetReviews = store.values.filter(r => r.targetType == targetType && r.targetId == targetId).toList
        if (targetReviews.isEmpty) {
          Some(ReviewRatingSummary(targetType, targetId, None, 0)).pure[F]
        } else {
          val avg = targetReviews.map(_.rating.value).map(_.toDouble).sum / targetReviews.size
          Some(ReviewRatingSummary(targetType, targetId, Some(avg), targetReviews.size)).pure[F]
        }
      }
    }.pure[F]
  }
}