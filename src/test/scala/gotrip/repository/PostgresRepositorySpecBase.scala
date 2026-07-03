package gotrip.repository

import cats.effect.{IO, Resource}
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import fs2.io.net.Network
import gotrip.config.{ConnectionPoolConfig, DatabaseConfig}
import gotrip.database.{Migration, SkunkSessionPool}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import org.testcontainers.containers.PostgreSQLContainer
import skunk.Session

import java.sql.{Connection, DriverManager}

trait PostgresRepositorySpecBase extends BeforeAndAfterAll with BeforeAndAfterEach:
  this: Suite =>

  private val postgres = new ScalaPostgreSQLContainer("postgres:17")
    .withDatabaseName("gotrip")
    .withUsername("gotrip_user")
    .withPassword("gotrip_password")

  private var poolRelease: IO[Unit] = IO.unit
  protected var sessionPool: Resource[IO, Session[IO]] =
    Resource.eval(IO.raiseError(new IllegalStateException("session pool is not ready")))

  override protected def beforeAll(): Unit =
    super.beforeAll()
    postgres.start()
    val config = databaseConfig
    Migration.migrate[IO](config).unsafeRunSync()
    val allocated = SkunkSessionPool[IO](config).allocated.unsafeRunSync()
    sessionPool = allocated._1
    poolRelease = allocated._2

  override protected def afterEach(): Unit =
    truncateApplicationTables()
    super.afterEach()

  override protected def afterAll(): Unit =
    try poolRelease.unsafeRunSync()
    finally
      postgres.stop()
      super.afterAll()

  protected def databaseConfig: DatabaseConfig =
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

  private def truncateApplicationTables(): Unit =
    withConnection { connection =>
      val tableNames = applicationTables(connection)
      if tableNames.nonEmpty then
        val quotedTables = tableNames.map(name => s""""public"."$name"""").mkString(", ")
        val statement = connection.createStatement()
        try statement.execute(s"truncate table $quotedTables restart identity cascade")
        finally statement.close()
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

  private def withConnection[A](use: Connection => A): A =
    val connection = DriverManager.getConnection(postgres.getJdbcUrl, postgres.getUsername, postgres.getPassword)
    try use(connection)
    finally connection.close()

private final class ScalaPostgreSQLContainer(imageName: String) extends PostgreSQLContainer[ScalaPostgreSQLContainer](imageName)
