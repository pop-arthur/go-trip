package gotrip.config

import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

case class AuthConfig(
    issuer: String,
    jwtSecret: String,
    accessTokenTtl: FiniteDuration,
    refreshTokenTtl: FiniteDuration,
    passwordCost: Int
) derives ConfigReader
