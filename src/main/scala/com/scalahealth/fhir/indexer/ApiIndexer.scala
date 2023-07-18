package com.scalahealth.fhir.indexer

import ca.uhn.fhir.rest.gclient.{ICriterion, IParam, TokenClientParam}
import com.scalahealth.fhir.client.{FHIRHapiClient, RequestMetadata}
import org.hl7.fhir.r4.model.{Bundle, Condition, Resource}
import zio.stream.{ZSink, ZStream}
import zio.{Chunk, Task, ZIO, ZLayer}

import scala.math.BigDecimal
import scala.reflect.ClassTag

trait ApiIndexer {
  protected val hapiFhirClient: FHIRHapiClient

  protected def persist(patientId: String, processedPages: Chunk[String]): Task[Unit]

  protected def indexApi[T <: IParam](searchMetadata: RequestMetadata[T]): ZIO[Any, Throwable, Unit]

  protected def createPagedApiIndexingFlow[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[Unit] = {
    for {
      bundle <- hapiFhirClient.executeSearch[R, T](searchMetadata)
      pagedResponses <- getPaginationZstream(bundle)
      _ <- ZIO.log(s"Processed api: ${getClass.getSimpleName} with ${pagedResponses.size} entries")
      wr <- persist(searchMetadata.patientId, pagedResponses)
    } yield wr
  }

  private def getPaginationZstream(firstPage: Bundle): ZIO[Any, Throwable, Chunk[String]] = {
    ZStream.paginateChunkZIO(firstPage) { bundle =>
      val entriesChunk = Chunk.fromIterable(hapiFhirClient.serializeBundle(bundle))
      hapiFhirClient.loadNextPage(bundle).map { nextBundleOpt =>
        Tuple2(entriesChunk, nextBundleOpt)
      }
    }.run(ZSink.collectAll[String])
  }
}


// TODO an example for usage, delete it
class ConditionIndexer(override val hapiFhirClient: FHIRHapiClient) extends ApiIndexer {
  override protected def persist(patientId: String, processedPages: Chunk[String]): Task[Unit] =
    ZIO.attempt(println(s"Persisting ${processedPages.size} pages for patient $patientId"))

  override protected def indexApi[T <: IParam](searchMetadata: RequestMetadata[T]): ZIO[Any, Throwable, Unit] = {
    createPagedApiIndexingFlow[Condition, T](searchMetadata)
  }
}

object ConditionIndexer {
  val layer = ZLayer {
    for {
      hapiClient <- ZIO.service[FHIRHapiClient]
    } yield new ConditionIndexer(hapiClient)
  }
}
