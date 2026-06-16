package gotrip.database

import cats.effect.{Async, Resource}
import cats.effect.std.Console
import fs2.io.net.Network
import gotrip.config.DatabaseConfig
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object SkunkSessionPool:
  def apply[F[_]: Async: Console: Network](
    config: DatabaseConfig
  ): Resource[F, Resource[F, Session[F]]] =
    given Meter[F] = Meter.noop[F]
    given Tracer[F] = Tracer.noop[F]

    Session.pooled[F](
      host = config.host,
      port = config.port,
      user = config.user,
      database = config.database,
      password = Some(config.password),
      max = config.connectionPool.maxSize
    )
