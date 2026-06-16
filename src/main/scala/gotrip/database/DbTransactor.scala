package gotrip.database

import cats.effect.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import gotrip.config.DatabaseConfig

object DbTransactor {
  def apply[F[_]: Async](config: DatabaseConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](config.connectionPool.maxSize)
      xa <- HikariTransactor.newHikariTransactor[F](
        driverClassName = config.driver,
        url             = config.url,
        user            = config.user,
        pass            = config.password,
        connectEC       = ce
      )
      _ <- Resource.eval(xa.configure { dataSource =>
        Async[F].delay {
          dataSource.setMaximumPoolSize(config.connectionPool.maxSize)
          dataSource.setMinimumIdle(config.connectionPool.minimumIdle)
        }
      })
    } yield xa
}