/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ofstedformsproxy.connectors

import akka.actor.ActorSystem
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSClient
import play.mvc.Http.Status
import scalaz.{-\/, \/, \/-}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.http.HttpProxyClient
import uk.gov.hmrc.ofstedformsproxy.models._
import uk.gov.hmrc.ofstedformsproxy.service.SOAPMessageService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._

@ImplementedBy(classOf[CygnumConnectorImpl])
trait CygnumConnector {

  def getUrn() : String \/ Option[URN]
  def send()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[SubmissionFailure \/ SubmissionSuccess]
}

@Singleton
class CygnumConnectorImpl @Inject()(auditConnector: AuditConnector,
                                    soapMessageService : SOAPMessageService,
                                    metrics: Metrics,
                                    wsClient: WSClient,
                                    system: ActorSystem)(appConfig: AppConfig) extends CygnumConnector {

  val logger: Logger = Logger(this.getClass())

  //TODO: get the proxy config from the config
  val proxyClient: HttpProxyClient = new HttpProxyClient(auditConnector, appConfig.runModeConfiguration, wsClient, "microservice.services.cygnum.proxy")

  //TODO: Get the URL to call from config
  private val cygnumUrl = "https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc"

  private val foo = "<s:Envelopexmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"xmlns:a=\"http://www.w3.org/2005/08/addressing\"xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"xmlns:u=\"http://docs.oasis-open.\\\norg/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><s:Header><a:Actions:mustUnderstand=\"1\">http://tempuri.org/IGatewayOOServices/GetData</a:Action><a:MessageID>urn:uuid:88d6297d-4\\\n390-4c81-91ae-89ad27f9cff8</a:MessageID><a:ReplyTo><a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo><a:Tos:mustUnderstand=\"1\"u:Id=\"_1\">https://testinfogatewa\\\ny.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc</a:To><o:Securityxmlns:o=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"s:mustUnderstand=\"1\"><u:Timest\\\nampu:Id=\"_0\"><u:Created>2019-01-22T15:48:17.817Z</u:Created><u:Expires>2019-01-22T15:53:17.822Z</u:Expires></u:Timestamp><o:UsernameTokenu:Id=\"uuid-9462075e-f36c-4c0f-a5ff-00d1d4812d1f-1\"><\\\no:Username>extranet\\hmrcgforms</o:Username><o:Passwordo:Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">MX(6ZvLS7wmt~2\\</o:Password></\\\no:UsernameToken><o:BinarySecurityTokenEncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\"ValueType=\"http://docs.oasis-open.org/wss/\\\n2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"u:Id=\"uuid-9462075e-f36c-4c0f-a5ff-00d1d4812d1f-2\">MIIG6DCCBdCgAwIBAgIKYbOINAABAAABfTANBgkqhkiG9w0BAQsFADBWMRUwEwYKCZImiZPyLGQBGRYFbG9\\\njYWwxGDAWBgoJkiaJk/IsZAEZFghFeHRyYW5ldDEjMCEGA1UEAxMaT2ZzdGVkIEV4dHJhbmV0IElzc3VpbmcgQ0EwHhcNMTgxMDEyMTEzMDUxWhcNMjAxMDExMTEzMDUxWjB/MRUwEwYKCZImiZPyLGQBGRYFbG9jYWwxGDAWBgoJkiaJk/IsZAEZFghF\\\neHRyYW5ldDEfMB0GA1UECxMWRXh0cmFuZXQgVXNlciBBY2NvdW50czEVMBMGA1UECxMMT25saW5lT2ZzdGVkMRQwEgYDVQQDEwtITVJDIGdGb3JtczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJhnuGJkBCHJIhXBtpYlpOcbxGsm569tE\\\n4MUstVbgI1oR0BOufQD+3czqgmqqevO+eHPa/U7WiawzkA8+GwO6+elL09fhVszT0HRjhzFAS7rFV7DwF2RWUBXuI5gkYe0O1C9Q05GqgSrrGuqoNodlrvVMDnDevo/hGmmL2sPxHqxSydNZvjrR4XTL5JHl41vFZX1zJpSMcZPiGYemLyyo2Z6YiGF5B\\\n8t4hJ+cyINxOlmA+HFjTHPbyuaeFfXGjaty9hKLum/53gvrSuYfxLbaFhdUfzKli2BvPUxnKwMRBBEg1Ft0a+6ZGOsLBV+tm0YlcCMCjAv3taas+Db75az1bsCAwEAAaOCA40wggOJMDwGCSsGAQQBgjcVBwQvMC0GJSsGAQQBgjcVCIP+oTO+80KEgYM\\\nShr/CToH0oT84g8eLc4WPkR8CAWQCAQgwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDgYDVR0PAQH/BAQDAgeAMBsGCSsGAQQBgjcVCgQOMAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFC4T+kfDVNCpCXPCAXxZDUPYIyb9MB8GA1UdIwQYMBaAFBu+fol7VANG\\\nbE0RDQ/MzUHp3IK/MIIBPQYDVR0fBIIBNDCCATAwggEsoIIBKKCCASSGgc5sZGFwOi8vL0NOPU9mc3RlZCUyMEV4dHJhbmV0JTIwSXNzdWluZyUyMENBKDEpLENOPUNFUlQxRVhULENOPUNEUCxDTj1QdWJsaWMlMjBLZXklMjBTZXJ2aWNlcyxDTj1TZ\\\nXJ2aWNlcyxDTj1Db25maWd1cmF0aW9uLERDPUV4dHJhbmV0LERDPWxvY2FsP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3Q/YmFzZT9vYmplY3RDbGFzcz1jUkxEaXN0cmlidXRpb25Qb2ludIZRaHR0cDovL2NlcnQxZXh0LmV4dHJhbmV0LmxvY2FsL0\\\nNlcnRFbnJvbGwvT2ZzdGVkJTIwRXh0cmFuZXQlMjBJc3N1aW5nJTIwQ0EoMSkuY3JsMIIBTgYIKwYBBQUHAQEEggFAMIIBPDCBwgYIKwYBBQUHMAKGgbVsZGFwOi8vL0NOPU9mc3RlZCUyMEV4dHJhbmV0JTIwSXNzdWluZyUyMENBLENOPUFJQSxDTj1\\\nQdWJsaWMlMjBLZXklMjBTZXJ2aWNlcyxDTj1TZXJ2aWNlcyxDTj1Db25maWd1cmF0aW9uLERDPUV4dHJhbmV0LERDPWxvY2FsP2NBQ2VydGlmaWNhdGU/YmFzZT9vYmplY3RDbGFzcz1jZXJ0aWZpY2F0aW9uQXV0aG9yaXR5MHUGCCsGAQUFBzAChmlo\\\ndHRwOi8vY2VydDFleHQuZXh0cmFuZXQubG9jYWwvQ2VydEVucm9sbC9DRVJUMUVYVC5FeHRyYW5ldC5sb2NhbF9PZnN0ZWQlMjBFeHRyYW5ldCUyMElzc3VpbmclMjBDQSgxKS5jcnQwNAYDVR0RBC0wK6ApBgorBgEEAYI3FAIDoBsMGUhNUkNnRm9yb\\\nXNARXh0cmFuZXQubG9jYWwwDQYJKoZIhvcNAQELBQADggEBAIwCaO7gSQPs6T1n/ZUlcLaceuGi1fVFXbKpTd25ulybUvl5BYQF/yBLMG12ShEiAlhn+dP1MKjIk6NMgDMEG/2d8g0Lano4gMBiXIhNqkZxVY4f2GIH4Oi8NTOMGUn6A9Qq5H5lR4bE7f\\\nT/BPOtAqkmrEbhQZ7WT/OgdeKg2C9gAiPt0oHHPt0Bw67whpvrJTnE9Q1vNhNwsq5aiqx2pdmdG2XwVB2BI+ByGCa7K0VSCKuZ3lX81GuFl/nb8iWRg5JTyK6zujOtJhzNJWKmtDDigW7D2eOYBkSCEd1ywde5mI1lsbchqHK1B2k5ZpxNsOB9B6Xfi4W\\\neRUaRyknvFds=</o:BinarySecurityToken><Signaturexmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethodAlgorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><SignatureM\\\nethodAlgorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ReferenceURI=\"#_0\"><Transforms><TransformAlgorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></Transforms><DigestMethodAlgorit\\\nhm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><DigestValue>+moQ7jSbsEEsRyoImY+GURw51C8=</DigestValue></Reference><ReferenceURI=\"#_1\"><Transforms><TransformAlgorithm=\"http://www.w3.org/2001/1\\\n0/xml-exc-c14n#\"/></Transforms><DigestMethodAlgorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><DigestValue>cXD1SfpOszifeqdm29v/oqAc5aY=</DigestValue></Reference></SignedInfo><SignatureVal\\\nue>h4Hr4IPps1irrhUmwZH9yPg2XQwTv4vXAYdnTrZAx2uctgVwH1IN6jMVWlyMxEy6DWljbBZZ03STlUQXQTIksOFPAumudvRK2lQ6BIaZmtx/f5UcQvO3yn/oKO56r8wQoCVVxwHKFpyoRu66QJq+1CwjcMTk/DV5hFnLDssjqIyg7S8VKyVnfFqvzp\\\no2/IJ8yt+z3UwKC9rLxqqVaJgZ37UF31Sc9LJgCbVmD/96ptkLWjxC7Hc+wZBMD/zz6qPenR1Oz/oNDiBnd0GJJyn+SJlIpzGHVdebyNDkiOq4mnBHVOSj3qfbZlqP897rwU1m0H5f4E717rCy0+Iio6NBWA==</SignatureValue><KeyInfo><o:Se\\\ncurityTokenReference><o:ReferenceURI=\"#uuid-9462075e-f36c-4c0f-a5ff-00d1d4812d1f-2\"ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"/></o:Sec\\\nurityTokenReference></KeyInfo></Signature></o:Security></s:Header><s:Bodyu:Id=\"_1\"><GetDataxmlns=\"http://tempuri.org/\"><Service>GetNewUrn</Service><InputParameters>&lt;?xmlversion=\"1.0\"enco\\\nding=\"utf-8\"?&gt;&lt;Parameters&gt;&lt;IDs&gt;&lt;ID&gt;SC&lt;/ID&gt;&lt;/IDs&gt;&lt;/Parameters&gt;</InputParameters></GetData></s:Body></s:Envelope>"

  def getUrn(): String \/ Option[URN] = {

    val result = for {
      document <- soapMessageService.readInXMLPayload("conf/xml/GetNewURN.xml")
      soapMessage <- soapMessageService.createSOAPEnvelope(document)
      signedSoapMessage <- soapMessageService.signSOAPMessage(soapMessage)
      _ <- soapMessageService.outputFile(signedSoapMessage)
      _ <- soapMessageService.call(signedSoapMessage)

    } yield ()

    result match {
      case \/-(d) => \/-(Some(URN("got document")))
      case -\/(e) => -\/(e.message)
    }

    //SOAP Service calls should be here
    // so instead of os.getUrn, the for comprehension should be here
    /*

        for {
          document <- ss.readXMLPayload()
          message <- ss.createMessage
          signedMessage <- ss.londosignMessage
          urn <- ss.call(signedMessage)
        } yield urn
     */

//    os.getUrn() match {
//      case \/-(GetUrnSuccess(urn)) => Some(URN("1232432"))
//      case -\/(GetUrnFailure(a)) => None
//    }

  }

  //TODO: manage the response status
  //TODO: how to send the complete SOAP envelope in the body - this needs to be a string (SOAP Envelope + Data)
  //TODO: need to call GetURN service first
  //TODO: need to construct the payload
  //TODO: need to read the response XML and see if the status is 0 or not so you could get a 200 HTTP but a status of 1 (which is a failure)
  //TODO: parameters should be none
  override def send()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionFailure \/ SubmissionSuccess] = {
    proxyClient.post(cygnumUrl, foo, Map("Content-Type" -> "application/soap+xml; charset=utf-8")).map[SubmissionFailure \/ SubmissionSuccess] {
      response =>
        response.status match {
          case Status.OK => {
            logger.info("Ok")
            \/-(SubmissionSuccess(""))
          }
          case Status.BAD_REQUEST => {
            logger.info("Bad Request")
            -\/(SubmissionFailure(""))
          }
          case Status.FORBIDDEN => {
            logger.info("forbidden")
            -\/(SubmissionFailure(""))
          }
          case Status.SERVICE_UNAVAILABLE => {
            logger.info("Service unavailable")
            -\/(SubmissionFailure(""))
          }
          case Status.GATEWAY_TIMEOUT => {
            logger.info("Gateway Timeout")
            -\/(SubmissionFailure(""))
          }
          case Status.INTERNAL_SERVER_ERROR => -\/(SubmissionFailure(""))
          case _ => {
            logger.info("Internal server error")
            -\/(SubmissionFailure(""))
          }
        }
    }.recover {
      case e =>
        -\/(SubmissionFailure(""))
    }
  }

}
