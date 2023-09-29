package io.github.royashcenazi.fhir.indexer.client

import io.github.royashcenazi.fhir.indexer.config.FhirIndexingConfig
import pdi.jwt.JwtAlgorithm.RS256
import pdi.jwt.{Jwt, JwtClaim}
import zio.http._
import zio.json._
import zio.{&, Task, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID


trait FhirAuthClient {
  protected val config: FhirIndexingConfig
  protected val zioClient: Client

  protected def setup(): Int = {
    java.security.Security.addProvider(
      new org.bouncycastle.jce.provider.BouncyCastleProvider()
    );
  }

  case class AccessToken(access_token: String)
  object AccessToken {
    implicit val decoder: JsonDecoder[AccessToken] = DeriveJsonDecoder.gen[AccessToken]
  }

  def getAccessToken: Task[String] = {
    val token = getJwtToken
    val urlEncodedMediaType: MediaType = MediaType.application.`x-www-form-urlencoded`

    val urlEncodedFormData: Body = Body.fromURLEncodedForm(Form(
      FormField.Simple("grant_type", "client_credentials"),
      FormField.Simple("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
      FormField.Simple("client_assertion", token)
    ))

    val url = URL.decode(config.authUrl).fold(
      _ => throw new Exception(s"failed to decode url ${config.authUrl}"),
      value => value
    )

    val request = Request.post(urlEncodedFormData, url)
      .addHeader(Header.ContentType(urlEncodedMediaType))

    for {
      response <- zioClient.request(request)
      body <- response.body.asString
    } yield {
      val decoded = body.fromJson[AccessToken]
      decoded.fold(
        _ => throw new Exception("Error decoding response"),
        value => value.access_token
      )
    }
  }

  private def getJwtToken: String = {
    val id = UUID.randomUUID().toString
    val fiveMinFromNow = Instant.now().plusSeconds(300).getEpochSecond
    val claim = JwtClaim(issuer = Some(config.clientId), subject = Some(config.clientId), audience = Some(Set(config.authUrl)),
      jwtId = Some(id), expiration = Some(fiveMinFromNow))
    Jwt.encode(claim, config.secret, RS256)
  }
}

  class FhirAuthClientImpl(override val config: FhirIndexingConfig, override val zioClient: Client) extends FhirAuthClient {
    setup()
  }

  object FhirAuthClientImpl {
    def layer(): ZLayer[Client & FhirIndexingConfig, Throwable, FhirAuthClientImpl] =
      ZLayer {
        for {
          client <- ZIO.service[Client]
          config <- ZIO.service[FhirIndexingConfig]
        } yield {
          new FhirAuthClientImpl(config, client)
        }
      }
}
