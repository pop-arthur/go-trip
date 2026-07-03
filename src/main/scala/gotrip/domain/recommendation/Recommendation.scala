package gotrip.domain.recommendation

import gotrip.domain.additionalservice.AdditionalService

final case class Recommendation(
  service: AdditionalService,
  reason: String,
  score: Option[Double]
)
