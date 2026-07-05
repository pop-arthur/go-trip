package gotrip.repository

import cats.effect.{IO, Resource}
import cats.effect.std.{Console, Semaphore}
import fs2.io.net.Network
import gotrip.config.{ConnectionPoolConfig, DatabaseConfig}
import gotrip.database.{Migration, SkunkSessionPool}
import munit.AnyFixture
import munit.CatsEffectSuite
import org.testcontainers.containers.PostgreSQLContainer
import skunk.Session

import java.sql.{Connection, DriverManager}

trait PostgresRepositorySpecBase extends CatsEffectSuite:

  private val databaseFixture = ResourceSuiteLocalFixture("postgres", postgresResource)
  private val testGateFixture = ResourceSuiteLocalFixture("repository test gate", Resource.eval(Semaphore[IO](1)))

  override def munitFixtures: Seq[AnyFixture[?]] =
    List(databaseFixture, testGateFixture)

  protected def sessionPool: Resource[IO, Session[IO]] =
    databaseFixture().sessionPool

  protected def repositoryTest(name: String)(body: => IO[Unit]): Unit =
    test(name) {
      testGateFixture().permit.use { _ =>
        body.guarantee(databaseFixture().truncate)
      }
    }

  private def postgresResource: Resource[IO, RepositoryDatabase] =
    for
      postgres <- Resource.make(IO.blocking {
        val container = new ScalaPostgreSQLContainer("postgres:17")
          .withDatabaseName("gotrip")
          .withUsername("gotrip_user")
          .withPassword("gotrip_password")
        container.start()
        container
      })(container => IO.blocking(container.stop()))
      config = databaseConfig(postgres)
      _ <- Resource.eval(Migration.migrate[IO](config))
      allocated <- SkunkSessionPool[IO](config)
    yield RepositoryDatabase(
      sessionPool = allocated,
      truncate = truncateApplicationTables(postgres)
    )

  private def databaseConfig(postgres: PostgreSQLContainer[?]): DatabaseConfig =
    DatabaseConfig(
      url = postgres.getJdbcUrl,
      host = postgres.getHost,
      port = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
      database = postgres.getDatabaseName,
      user = postgres.getUsername,
      password = postgres.getPassword,
      driver = "org.postgresql.Driver",
      connectionPool = ConnectionPoolConfig(maxSize = 4, minimumIdle = 1)
    )

  private def truncateApplicationTables(postgres: PostgreSQLContainer[?]): IO[Unit] =
    connectionResource(postgres).use { connection =>
      IO.blocking {
        val tableNames = applicationTables(connection)

        if tableNames.nonEmpty then
          val quotedTables = tableNames.map(name => s""""public"."$name"""").mkString(", ")
          val statement = connection.createStatement()
          try statement.execute(s"truncate table $quotedTables restart identity cascade")
          finally statement.close()
      }
    }  

  private def applicationTables(connection: Connection): List[String] =
    val resultSet = connection.getMetaData.getTables(null, "public", "%", Array("TABLE"))
    try
      val tables = List.newBuilder[String]
      while resultSet.next() do
        val tableName = resultSet.getString("TABLE_NAME")
        if tableName != "flyway_schema_history" then tables += tableName
      tables.result()
    finally resultSet.close()

  private def connectionResource(postgres: PostgreSQLContainer[?]): Resource[IO, Connection] =
    Resource.make {
      IO.blocking {
        DriverManager.getConnection(
          postgres.getJdbcUrl,
          postgres.getUsername,
          postgres.getPassword
        )
      }
    } { connection =>
      IO.blocking(connection.close())
  }

private final case class RepositoryDatabase(
  sessionPool: Resource[IO, Session[IO]],
  truncate: IO[Unit]
)

private final class ScalaPostgreSQLContainer(imageName: String) extends PostgreSQLContainer[ScalaPostgreSQLContainer](imageName)
