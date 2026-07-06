package gotrip.service

import cats.effect.IO
import org.scalamock.scalatest.MockFactory

import java.time.Instant
import java.util.UUID

trait GeneratedDataTestSupport:
  self: MockFactory =>

  protected def generatedDataMock: GeneratedData[IO] =
    mock[GeneratedData[IO]]

  protected def expectGeneratedId(generatedData: GeneratedData[IO], id: UUID): Unit =
    (() => generatedData.newId()).expects().returning(IO.pure(id))

  protected def expectGeneratedNow(generatedData: GeneratedData[IO], now: Instant): Unit =
    (() => generatedData.now()).expects().returning(IO.pure(now))
