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
        .placeholders(adminPlaceholders)
        .load()
      flyway.migrate()
    }.void

  private def adminPlaceholders: java.util.Map[String, String] =
    val placeholders = new java.util.HashMap[String, String]()
    placeholders.put("gotrip.admin.email", sys.env.getOrElse("GOTRIP_ADMIN_EMAIL", ""))
    placeholders.put("gotrip.admin.password", sys.env.getOrElse("GOTRIP_ADMIN_PASSWORD", ""))
    placeholders.put("gotrip.admin.fullName", sys.env.getOrElse("GOTRIP_ADMIN_FULL_NAME", "GoTrip Admin"))
    placeholders
}
