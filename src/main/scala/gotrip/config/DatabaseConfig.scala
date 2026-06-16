package gotrip.config

import pureconfig.ConfigReader

case class DatabaseConfig(
    url: String,
    user: String,
    password: String,
    driver: String,
    connectionPool: ConnectionPoolConfig
) derives ConfigReader

case class ConnectionPoolConfig(
    maxSize: Int,
    minimumIdle: Int
) derives ConfigReader
