package gotrip.repository.review

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.user.UserId
import gotrip.domain.review._
import scala.collection.mutable

object InMemoryReviewRepository {
  def make[F[_]: Applicative]: F[ReviewRepository[F]] = {
    val store = mutable.Map.empty[ReviewId, Review]

    new ReviewRepository[F] {
      override def create(review: Review): F[Review] = {
        store += (review.id -> review)
        review.pure[F]
      }

      override def findById(id: ReviewId): F[Option[Review]] = store.get(id).pure[F]

      override def findByTarget(targetType: ReviewTargetType, targetId: ReviewTargetId): F[List[Review]] =
        store.values.filter(r => r.targetType == targetType && r.targetId == targetId).toList.pure[F]

      override def findByUserId(userId: UserId): F[List[Review]] =
        store.values.filter(_.userId == userId).toList.pure[F]

      override def update(review: Review): F[Int] = {
        store.get(review.id) match {
          case Some(_) =>
            store += (review.id -> review)
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
          case list => Some(list.sum.toDouble / list.size).pure[F]
        }
      }
    }.pure[F]
  }
}
