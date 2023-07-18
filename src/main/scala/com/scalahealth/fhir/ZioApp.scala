package com.scalahealth.fhir

import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.scalahealth.fhir.client.{FHIRHapiClient, FHIRHapiClientImpl, RequestMetadata}
import com.scalahealth.fhir.config.ScalaHealthFhirConfig
import org.hl7.fhir.r4.model.{Bundle, Observation}
import zio.{Exit, Runtime, Scope, Unsafe, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}

object ZioApp extends ZIOAppDefault {
  private val requestMetadata = RequestMetadata[TokenClientParam]("123")

  def myApp: ZIO[FHIRHapiClient, Nothing, Exit[Throwable, Bundle]] = for {
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
        FHIRHapiClientImpl.layer())
  }
}

