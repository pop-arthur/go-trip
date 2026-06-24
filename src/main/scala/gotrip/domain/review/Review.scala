package gotrip.domain.review

import java.time.Instant
import gotrip.domain.user.UserId

final case class Review(
  id: ReviewId,
  userId: UserId,
  targetType: ReviewTargetType,
  targetId: ReviewTargetId,
  rating: ReviewRating,
  text: ReviewText,
  createdAt: Instant,
  updatedAt: Instant
)