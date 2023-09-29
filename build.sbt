import Dependencies._


lazy val root = (project in file("."))
  .settings(
    name := "fhir-indexer",
    scalaVersion := "2.13.11",
    version := "0.1.0-SNAPSHOT",
    organization := "io.github.royashcenazi",
    libraryDependencies ++=
      Seq(munit, happyFhirClient, r4Model, happyFhirBase, jwt,
        jackson, javaSecurity) ++ zio
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
