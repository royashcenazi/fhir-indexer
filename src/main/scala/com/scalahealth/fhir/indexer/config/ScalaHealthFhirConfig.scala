package com.scalahealth.fhir.indexer.config

case class ScalaHealthFhirConfig(url: String)

object ScalaHealthFhirConfig {
  val layer: zio.ZLayer[Any, Nothing, ScalaHealthFhirConfig] =
    zio.ZLayer.succeed(ScalaHealthFhirConfig(url = "https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4"))
}
