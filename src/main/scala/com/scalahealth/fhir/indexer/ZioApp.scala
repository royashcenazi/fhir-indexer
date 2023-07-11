package com.scalahealth.fhir.indexer
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.scalahealth.fhir.indexer.client.{FHIRHapiClient, FHIRHapiClientImpl, RequestMetadata}
import com.scalahealth.fhir.indexer.config.ScalaHealthFhirConfig
import org.hl7.fhir.r4.model.Observation
import zio.{Runtime, Scope, Unsafe, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}

object ZioApp extends ZIOAppDefault {
  private val requestMetadata = RequestMetadata[TokenClientParam]("123")

  def myApp = for {
    zio <- ZIO.serviceWith[FHIRHapiClient](_.executeSearch[Observation, TokenClientParam](requestMetadata))
  } yield {
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(zio)
    }
  }

  object ScalaHealthFhirConfigTest {
    val layer: zio.ZLayer[Any, Nothing, ScalaHealthFhirConfig] =
      zio.ZLayer.succeed(ScalaHealthFhirConfig(url = "https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4"))
  }


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    myApp.debug("stam")
      .provide(ScalaHealthFhirConfigTest.layer,
        FHIRHapiClientImpl.layer)
  }
}

