package com.scalahealth.fhir.indexer

import ca.uhn.fhir.rest.gclient.{ICriterion, IParam, TokenClientParam}
import com.scalahealth.fhir.client.{FHIRHapiClient, RequestMetadata}
import org.hl7.fhir.r4.model.{Bundle, Condition, Resource}
import zio.stream.{ZSink, ZStream}
import zio.{Chunk, Task, ZIO, ZLayer}

import scala.reflect.ClassTag

trait ApiIndexer {
  type ProcessResult
  protected val hapiFhirClient: FHIRHapiClient

  def indexApi[T <: IParam](searchMetadata: RequestMetadata[T]): ZIO[Any, Throwable, ProcessResult]

  protected def createPagedApiIndexingFlow[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[Chunk[String]] = {
    for {
      bundle <- hapiFhirClient.executeSearch[R, T](searchMetadata)
      pagedResponses <- getPaginationZstream(bundle)
      _ <- ZIO.log(s"Processed api: ${getClass.getSimpleName} with ${pagedResponses.size} entries")
    } yield pagedResponses
  }

  protected def createSinglePageFlow[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[String] = {
    for {
      resource <- hapiFhirClient.executeRead[R, T](searchMetadata)
      _ <- ZIO.log(s"Processed single page api: ${getClass.getSimpleName}")
      parsedResource = hapiFhirClient.serializeResource(resource)
    } yield parsedResource
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

