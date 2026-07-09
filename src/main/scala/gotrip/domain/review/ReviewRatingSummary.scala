package gotrip.domain.review

final case class ReviewRatingSummary(
  targetType: ReviewTargetType,
  targetId: ReviewTargetId,
  averageRating: Option[Double],
  reviewCount: Int
)