package gotrip.service

import cats.Functor
import cats.effect.{Clock, Sync}
import cats.syntax.functor.*

import java.time.Instant
import java.util.UUID

trait GeneratedData[F[_]]:
  def newId(): F[UUID]
  def now(): F[Instant]

object GeneratedData:
  def apply[F[_]](using generatedData: GeneratedData[F]): GeneratedData[F] =
    generatedData

  given default[F[_]: Sync: Clock]: GeneratedData[F] with
    override def newId(): F[UUID] = GeneratedData.newId[F]
    override def now(): F[Instant] = GeneratedData.now[F]

  def newId[F[_]: Sync]: F[UUID] =
    Sync[F].delay(UUID.randomUUID())

  def now[F[_]: Clock: Functor]: F[Instant] =
    Clock[F].realTime.map(duration => Instant.ofEpochMilli(duration.toMillis))
