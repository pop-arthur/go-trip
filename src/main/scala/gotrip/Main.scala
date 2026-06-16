package gotrip

import cats.effect.{IO, IOApp}
import cats.syntax.either.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import gotrip.config.DatabaseConfig
import gotrip.database.{Migration, SkunkSessionPool}

object Main extends IOApp.Simple {

  def run: IO[Unit] =
    for {
      config <- IO.fromEither(
        ConfigSource.default.at("gotrip.database").load[DatabaseConfig]
          .leftMap(e => ConfigReaderException[DatabaseConfig](e))
      )

      _ <- IO.println("Running Flyway migrations...")
      _ <- Migration.migrate[IO](config)
      
      _ <- IO.println("Initializing Skunk session pool...")
      _ <- SkunkSessionPool[IO](config).use { _ =>
        for {
          _ <- IO.println("Database module ready. Exiting for now (no server yet).")
        } yield ()
      }
    } yield ()
}
