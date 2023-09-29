import Dependencies._


lazy val root = (project in file("."))
  .settings(
    name := "fhir-indexer",
    scalaVersion := "2.13.11",
    version := "v0.0.1",
    organization := "io.github.royashcenazi",
    libraryDependencies ++=
      Seq(munit, happyFhirClient, r4Model, happyFhirBase, jwt,
        jackson, javaSecurity) ++ zio
  )

developers := List(
  Developer(
    id = "royashcenazi",
    name = "Roy Ashcenazi",
    email = "royashcenazi@gmail.com",
    url = url("https://github.com/royashcenazi")
  )
)

licenses += ("MIT", url("https://opensource.org/license/mit/"))
scmInfo := Some(ScmInfo(url("https://github.com/royashcenazi/fhir-indexer"), "git@github.com/royashcenazi/fhir-indexer.git"))
publishMavenStyle := true
homepage := Some(url("https://github.com/royashcenazi/fhir-indexer"))
crossPaths := false
isSnapshot := version.value.endsWith("-SNAPSHOT")
publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
pomIncludeRepository := { _ => false }

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
