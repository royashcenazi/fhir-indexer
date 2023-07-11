package com.scalahealth.fhir.indexer.client

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.{ICriterion, IGetPageTyped, IParam, IQuery, IReadExecutable, ReferenceClientParam}
import com.scalahealth.fhir.indexer.config.ScalaHealthFhirConfig
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.{Bundle, Resource}
import zio.{Task, ZIO, ZLayer}
import zio.json._

import scala.reflect.{ClassTag, classTag}

case class RequestMetadata[T <: IParam](patientId: String, additionalHeaders: Seq[(String, String)] = Seq(), additionalCriterions: Seq[ICriterion[T]] = Seq())

trait FHIRHapiClient {
  def executeSearch[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[Bundle]

  def executeRead[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[R]

  def loadNextPage(bundle: Bundle, additionalHeaders: (String, String)*): Task[Option[Bundle]]

  //
  //  def serializeResource[R <: Resource](r: R): String
  //
  //  def serializeBundle(bundle: Bundle): Seq[String]
  //
}

class FHIRHapiClientImpl(config: ScalaHealthFhirConfig, fhirCtx: FhirContext = FhirContext.forR4()) extends FHIRHapiClient {
  private val client: IGenericClient = fhirCtx.newRestfulGenericClient(config.url)
  private val pageLoader = client.loadPage()
  private val jsonParser = fhirCtx.newJsonParser()

  override def executeSearch[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[Bundle] = {
    ZIO.attempt {
      val basicQuery = client.search().forResource(classTag[R].runtimeClass.asInstanceOf[Class[R]])
        .where(new ReferenceClientParam("patient").hasId(searchMetadata.patientId))

      basicQuery.withAdditionalCond(searchMetadata.additionalCriterions)
        .withAdditionalHeaders(searchMetadata)
        .returnBundle(classOf[Bundle])
        .execute()
    }
  }

  override def executeRead[R <: Resource : ClassTag, T <: IParam](requestMetadata: RequestMetadata[T]): Task[R] = {
    ZIO.attempt {
      val basicQuery: IReadExecutable[R] = client.read().resource(classTag[R].runtimeClass.asInstanceOf[Class[R]])
        .withId(requestMetadata.patientId)

      basicQuery.withAdditionalHeaders(requestMetadata)
        .execute()
    }
  }

  override def loadNextPage(bundle: Bundle, additionalHeaders: (String, String)*): Task[Option[Bundle]] = {
    ZIO.attempt {
      Option.when(bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
        val request: IGetPageTyped[Bundle] = pageLoader.next(bundle)

        request.withAdditionalHeaders(additionalHeaders: _*)
          .execute()
      }
    }
  }

  private implicit class ReadExecutableOps[R <: Resource](readExecutable: IReadExecutable[R]) {
    def withAdditionalHeaders[T <: IParam](requestMetadata: RequestMetadata[T]): IReadExecutable[R] = {
      requestMetadata.additionalHeaders
        .foldLeft(readExecutable)({ case (query, (headerName, headerValue)) =>
          query.withAdditionalHeader(headerName, headerValue)
        })
    }
  }

  private implicit class QueryExecutableOps[R <: Resource](queryExecutable: IQuery[Nothing]) {

    def withAdditionalCond[T <: IParam](additionalCriterions: Seq[ICriterion[T]]): IQuery[Nothing] = {
      additionalCriterions.foldLeft(queryExecutable)((query, criterion) => query.and(criterion))
    }

    def withAdditionalHeaders[T <: IParam](requestMetadata: RequestMetadata[T]): IQuery[Nothing] = {
      requestMetadata.additionalHeaders.foldLeft(queryExecutable)({ case (query, (headerName, headerValue)) =>
        query.withAdditionalHeader(headerName, headerValue)
      })
    }
  }

  private implicit class PagingOps[R <: Resource](pagedReq: IGetPageTyped[R]) {
    def withAdditionalHeaders[T <: IParam](headers: (String, String)*): IGetPageTyped[R] = {
      headers.foldLeft(pagedReq)({ case (query, (headerName, headerValue)) =>
        query.withAdditionalHeader(headerName, headerValue)
      })
    }
  }
}


object FHIRHapiClientImpl {
  val layer: ZLayer[ScalaHealthFhirConfig, Nothing, FHIRHapiClient] =
    ZLayer {
      for {
        config <- ZIO.service[ScalaHealthFhirConfig]
      } yield {
        new FHIRHapiClientImpl(config)
      }
    }
}


//
//  override def executeRead[R <: Resource : ClassTag](patientId: String): Future[R] = ???
//
//  override def serializeResource[R <: Resource](r: R): String = ???
//
//  override def serializeBundle(bundle: Bundle): Seq[String] = ???
//
//  override def loadNextPage(bundle: Bundle): Future[Option[Bundle]] = ???
