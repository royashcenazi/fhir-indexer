package io.github.royashcenazi.fhir.indexer.client

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.{ICriterion, IGetPageTyped, IParam, IQuery, IReadExecutable, ReferenceClientParam}
import io.github.royashcenazi.fhir.indexer.config.FhirIndexingConfig
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.{Bundle, Resource}
import zio.http.Client
import zio.{&, Task, ZIO, ZLayer}
import zio.json._

import scala.jdk.javaapi.CollectionConverters
import scala.reflect.{ClassTag, classTag}

case class RequestMetadata[T <: IParam](patientId: String, additionalHeaders: Seq[(String, String)] = Seq(), additionalCriteria: Seq[ICriterion[T]] = Seq())

trait FHIRHapiClient {
  protected val config: FhirIndexingConfig
  protected val fhirContext: FhirContext
  protected val authClient: FhirAuthClient
  private val client: IGenericClient = fhirContext.newRestfulGenericClient(s"${config.url}/api/FHIR/R4")
  private val pageLoader = client.loadPage()
  private val jsonParser = fhirContext.newJsonParser()

  def executeSearch[R <: Resource : ClassTag, T <: IParam](searchMetadata: RequestMetadata[T]): Task[Bundle] = {
    for {
      token <- authClient.getAccessToken
      } yield {
      val basicQuery = client.search().forResource(classTag[R].runtimeClass.asInstanceOf[Class[R]])
        .where(new ReferenceClientParam("patient").hasId(searchMetadata.patientId))

      val query = basicQuery.withAdditionalCond(searchMetadata.additionalCriteria)
        .withAdditionalHeaders(searchMetadata)
        .returnBundle(classOf[Bundle])
        .withAdditionalHeader("Authorization", s"${getTokenField(token)}")

      query.execute()
    }
  }


  def executeRead[R <: Resource : ClassTag, T <: IParam](requestMetadata: RequestMetadata[T]): Task[R] = {
    for {
      token <- authClient.getAccessToken
    } yield {
      val basicQuery: IReadExecutable[R] = client.read().resource(classTag[R].runtimeClass.asInstanceOf[Class[R]])
        .withId(requestMetadata.patientId)
        .withAdditionalHeader("Authorization", s"${getTokenField(token)}")

      basicQuery.withAdditionalHeaders(requestMetadata)
        .execute()
    }
  }

  def loadNextPage(bundle: Bundle, additionalHeaders: (String, String)*): Task[Option[Bundle]] = {
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

    def withAdditionalCond[T <: IParam](additionalCriteria: Seq[ICriterion[T]]): IQuery[Nothing] = {
      additionalCriteria.foldLeft(queryExecutable)((query, criterion) => query.and(criterion))
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

  def serializeResource[R <: Resource](r: R): String = {
    jsonParser.encodeResourceToString(r)
  }

  def serializeBundle(bundle: Bundle): Seq[String] = {
    val entries = CollectionConverters.asScala(bundle.getEntry).map(_.getResource).toSeq
    entries.map(serializeResource)
  }

  protected def getTokenField(token: String) = s"Bearer $token"

}

class FHIRHapiClientImpl(override val config: FhirIndexingConfig, override val fhirContext: FhirContext, override val authClient: FhirAuthClient) extends FHIRHapiClient {}

object FHIRHapiClientImpl {
  def layer(fhirCtx: FhirContext = FhirContext.forR4()): ZLayer[FhirAuthClient & FhirIndexingConfig, Nothing, FHIRHapiClient] =
    ZLayer {
      for {
        fhirAuthClient <- ZIO.service[FhirAuthClient]
        config <- ZIO.service[FhirIndexingConfig]
      } yield {
        new FHIRHapiClientImpl(config, fhirCtx, fhirAuthClient)
      }
    }
}
