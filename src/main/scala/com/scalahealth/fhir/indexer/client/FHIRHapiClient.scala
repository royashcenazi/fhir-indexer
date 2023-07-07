package com.scalahealth.fhir.indexer.client

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.{ICriterion, IParam, ReferenceClientParam}
import com.scalahealth.fhir.indexer.config.ScalaHealthFhirConfig
import org.hl7.fhir.r4.model.{Bundle, Resource}
import zio.{ZIO, ZLayer}

import scala.reflect.{ClassTag, classTag}


trait FHIRHapiClient {
  def executeSearch[R <: Resource : ClassTag, T <: IParam](patientId: String, additionalCriterions: Seq[ICriterion[T]] = Seq()): Bundle

  //  def executeRead[R <: Resource : ClassTag](patientId: String): Future[R]
  //
  //  def serializeResource[R <: Resource](r: R): String
  //
  //  def serializeBundle(bundle: Bundle): Seq[String]
  //
  //  def loadNextPage(bundle: Bundle): Future[Option[Bundle]]
}

class FHIRHapiClientImpl(config: ScalaHealthFhirConfig) extends FHIRHapiClient {
  private val fhirCtx = FhirContext.forR4()
  private val client: IGenericClient = fhirCtx.newRestfulGenericClient(config.url)
  private val pageLoader = client.loadPage()
  private val jsonParser = fhirCtx.newJsonParser()

  override def executeSearch[R <: Resource : ClassTag, T <: IParam](patientId: String, additionalCriterions: Seq[ICriterion[T]]): Bundle = {
    val basicQuery = client.search().forResource(classTag[R].runtimeClass.asInstanceOf[Class[R]])
      .where(new ReferenceClientParam("patient").hasId(patientId))

    val withAdditionalCond = additionalCriterions.foldLeft(basicQuery)((query, criterion) => query.and(criterion))

    withAdditionalCond.returnBundle(classOf[Bundle])
      .execute()
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
