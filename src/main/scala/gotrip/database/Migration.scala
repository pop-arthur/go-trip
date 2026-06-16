package gotrip.database

import cats.effect.Sync
import cats.syntax.functor.*
import org.flywaydb.core.Flyway
import gotrip.config.DatabaseConfig

object Migration {
  def migrate[F[_]: Sync](config: DatabaseConfig): F[Unit] =
    Sync[F].delay {
      val flyway = Flyway.configure()
        .dataSource(config.url, config.user, config.password)
        .load()
      flyway.migrate()
    }.void
}