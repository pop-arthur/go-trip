ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"
ThisBuild / organization := "com.gotrip"

lazy val root = (project in file("."))
  .settings(
    name := "gotrip-backend",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.7.0",
      
      // "org.tpolecat" %% "doobie-core"     % "1.0.0-RC12",
      // "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC12",
      // "org.tpolecat" %% "doobie-hikari"   % "1.0.0-RC12",

      "org.tpolecat" %% "skunk-core" % "1.0.0",
      
      "org.flywaydb" % "flyway-core"                % "12.8.1",
      "org.flywaydb" % "flyway-database-postgresql" % "12.8.1",
      "org.postgresql" % "postgresql"               % "42.7.8",
      
      "com.github.pureconfig" %% "pureconfig-core"           % "0.17.10",
      "com.github.pureconfig" %% "pureconfig-generic-scala3" % "0.17.10",
      "com.typesafe"           % "config"                    % "1.4.9",
      
      "ch.qos.logback" % "logback-classic" % "1.5.34",
      "org.typelevel" %% "log4cats-slf4j"  % "2.8.0"
    )
  )
