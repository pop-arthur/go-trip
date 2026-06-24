ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"
ThisBuild / organization := "com.gotrip"

val tapirVersion = "1.13.21"
val http4sVersion = "0.23.17"
val circeVersion = "0.14.15"
val scalaTestVersion = "3.2.20"
val scalaMockVersion = "7.5.5"

lazy val root = (project in file("."))
  .settings(
    name := "gotrip-backend",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.7.0",
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalamock" %% "scalamock" % scalaMockVersion % Test,
      
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.github.jwt-scala" %% "jwt-circe" % "11.0.4",
      "org.mindrot" % "jbcrypt" % "0.4",

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
