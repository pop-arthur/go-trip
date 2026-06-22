package gotrip.config

import pureconfig.ConfigReader

case class ServerConfig(
    host: String,
    port: Int
) derives ConfigReader
