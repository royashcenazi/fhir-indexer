package io.github.royashcenazi.fhir.indexer

import ca.uhn.fhir.rest.gclient.{IParam, TokenClientParam}
import io.github.royashcenazi.fhir.indexer.client.{FHIRHapiClient, FHIRHapiClientImpl, FhirAuthClientImpl, RequestMetadata}
import io.github.royashcenazi.fhir.indexer.config.ScalaHealthFhirConfig
import io.github.royashcenazi.fhir.indexer.indexer.ApiIndexer
import org.hl7.fhir.r4.model.{Bundle, Condition, Observation}
import zio.http.{Body, Client, Response, ZClient}
import zio.{&, Exit, Runtime, Scope, Unsafe, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault, ZLayer}

// TODO an example for usage, delete it
object ZioApp extends ZIOAppDefault {

  class ConditionIndexer(override val hapiFhirClient: FHIRHapiClient) extends ApiIndexer {
    override type ProcessResult = Unit

    override def indexApi[T <: IParam](searchMetadata: RequestMetadata[T]): ZIO[Any, Throwable, Unit] = {
      for {
        data <- createPagedApiIndexingFlow[Condition, T](searchMetadata)
        _ <- ZIO.log(s"Processed api: ${getClass.getSimpleName} with ${data.size} entries")
      } yield ()
    }
  }

  object ConditionIndexer {
    val layer: ZLayer[FHIRHapiClient, Nothing, ConditionIndexer] = ZLayer {
      for {
        hapiClient <- ZIO.service[FHIRHapiClient]
      } yield new ConditionIndexer(hapiClient)
    }
  }

  def myApp: ZIO[ConditionIndexer, Nothing, Exit[Throwable, Unit]] = {
    val requestMetadata = RequestMetadata[TokenClientParam]("")
    for {
      conditionsService <- ZIO.service[ConditionIndexer]
      zio = conditionsService.indexApi[TokenClientParam](requestMetadata)
    } yield {
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run(zio)
      }
    }
  }

  object ScalaHealthFhirConfigTest {
    val layer: zio.ZLayer[Any, Nothing, ScalaHealthFhirConfig] =
      zio.ZLayer.succeed(ScalaHealthFhirConfig(url = "",
        authUrl ="/oauth2/token",
        clientId = "",
        secret = ""))
  }


  override def run: ZIO[Any, Throwable, Exit[Throwable, Unit]] = {
    myApp.debug("example")
      .provide(
        Client.default,
        ScalaHealthFhirConfigTest.layer,
        FhirAuthClientImpl.layer(),
        FHIRHapiClientImpl.layer(),
        ConditionIndexer.layer
      )
  }
}

