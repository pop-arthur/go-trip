package gotrip.service

import cats.Functor
import cats.effect.{Clock, Sync}
import cats.syntax.functor.*

import java.time.Instant
import java.util.UUID

object GeneratedData:
  def newId[F[_]: Sync]: F[UUID] =
    Sync[F].delay(UUID.randomUUID())

  def now[F[_]: Clock: Functor]: F[Instant] =
    Clock[F].realTime.map(duration => Instant.ofEpochMilli(duration.toMillis))
