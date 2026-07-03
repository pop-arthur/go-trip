package gotrip.http.recommendation

import gotrip.domain.recommendation.Recommendation
import gotrip.http.additionalservice.AdditionalServiceCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.derived

object RecommendationCodecs:
  given Encoder[Recommendation] =
    deriveEncoder

  given Decoder[Recommendation] =
    deriveDecoder

  given Schema[Recommendation] =
    derived
