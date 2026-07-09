package gotrip.integration.duffel

import pureconfig.ConfigReader

final case class DuffelConfig(
  baseUrl: String,
  accessToken: String,
  version: String = "v2"
) derives ConfigReader
