package com.scalahealth.fhir.indexer
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.scalahealth.fhir.indexer.client.{FHIRHapiClient, FHIRHapiClientImpl}
import com.scalahealth.fhir.indexer.config.ScalaHealthFhirConfig
import org.hl7.fhir.r4.model.Observation
import zio.{Scope, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}

object ZioApp extends ZIOAppDefault {

  def myApp = for {
    _ <- ZIO.serviceWith[FHIRHapiClient](_.executeSearch[Observation, TokenClientParam]("123"))
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    myApp.debug("stam")
      .provide(ScalaHealthFhirConfig.layer,
        FHIRHapiClientImpl.layer)
  }
}

