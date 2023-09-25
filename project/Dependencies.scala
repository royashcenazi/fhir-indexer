import sbt._

object Dependencies {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.29"
  val happyFhirClient = "ca.uhn.hapi.fhir" % "hapi-fhir-client" % "6.6.0"
  val r4Model = "ca.uhn.hapi.fhir" % "hapi-fhir-structures-r4" % "6.6.0"
  val logBack = "ch.qos.logback" % "logback-classic" % "1.4.6"
  val happyFhirBase = "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "6.6.0"
  val slf4j = "org.slf4j" % "slf4j-api" % "2.0.7"
  //TODO - remove this dependency and use the one from the feelbetter-plugin
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2"

  val jwt = "com.pauldijou" %% "jwt-core" % "5.0.0"

  val zio = Seq(
    "dev.zio" %% "zio" % "2.0.15",
    "dev.zio" %% "zio-streams" % "2.0.15",
    "dev.zio" %% "zio-http" % "3.0.0-RC2"
  )
  val javaSecurity = "org.bouncycastle" % "bcprov-jdk15on" % "1.70"

}
