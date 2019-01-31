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

import java.io._
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import javax.xml.soap.SOAPMessage
import play.api.Logger
import play.api.libs.ws.WSClient
import play.mvc.Http.Status
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.service.SOAPMessageService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, XML}

@ImplementedBy(classOf[CygnumConnectorImpl])
trait CygnumConnector {

  def getUrn()(implicit hc: HeaderCarrier, ex: ExecutionContext) : String \/ String

}

@Singleton
class CygnumConnectorImpl @Inject()(auditConnector: AuditConnector,
                                    soapMessageService : SOAPMessageService,
                                    metrics: Metrics,
                                    wsClient: WSClient,
                                    system: ActorSystem)(appConfig: AppConfig) extends CygnumConnector {

  val logger: Logger = Logger(this.getClass())

  //TODO: get the proxy config from the config
  //val proxyClient: HttpProxyClient = new HttpProxyClient(auditConnector, appConfig.runModeConfiguration, wsClient, "microservice.services.cygnum.proxy")
  val sp = SoapPost("microservice.services.cygnum.proxy")

  //TODO: Get the URL to call from config
  private val cygnumUrl = "https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc"

  def getUrn()(implicit hc: HeaderCarrier, ex: ExecutionContext): String \/ String= {
    //TODO: process the request body
    //TODO: call the post method via the connector interface
    //TODO: the payload body needs to be a string representation
    //TODO: construct the soap envelope
    //TODO: construct the message
    //TODO: process response
    //TODO: extract the urn

    val XML_FILE_PATH = "conf/xml/GetNewURN.xml"

    val result: String \/ String = for {
      xmlDocument       <- soapMessageService.readInXMLPayload(XML_FILE_PATH)
      soapMessage       <- soapMessageService.createSOAPEnvelope(xmlDocument)
      signedSoapMessage <- soapMessageService.signSOAPMessage(soapMessage)
      //response          <- EitherT.eitherT(send(signedSoapMessage))
    } yield convert(signedSoapMessage) //urn

    result match {
      case \/-(d) => \/-(d)
      case -\/(e) => -\/(e)
    }

  }

  def createTempFileForData(data: SOAPMessage): File = {
    val file = File.createTempFile(getClass.getSimpleName, ".tmp")
//    file.deleteOnExit() //FIXME
    val os = new FileOutputStream(file)
    try {
      data.writeTo(os)
      file
    } finally {
      os.close()
    }
  }

  def convert(soapMessage : SOAPMessage) : String = {
   val file = File.createTempFile(getClass.getSimpleName, ".tmp")
    val fos = new FileOutputStream(file)
    soapMessage.writeTo(fos)  // This produces a valid SOAP payload
    val xx = XML.loadFile(file)
    val writer = new StringWriter
    XML.write(writer, xx, StandardCharsets.UTF_8.toString, xmlDecl = true, null)
    val k = writer.toString
    println(k)
    k
  }


  //TODO: manage the response status
  //TODO: how to send the complete SOAP envelope in the body - this needs to be a string (SOAP Envelope + Data)
  //TODO: need to call GetURN service first
  //TODO: need to construct the payload
  //TODO: need to read the response XML and see if the status is 0 or not so you could get a 200 HTTP but a status of 1 (which is a failure)
  //TODO: parameters should be none
  def send(payload : SOAPMessage)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String \/ Elem] = {
    sp.post(cygnumUrl, convert(payload), Seq("Content-Type" -> "application/soap+xml; charset=utf-8")).map {
      response =>
        response.status match {
          case Status.OK => {
            logger.info("Ok")
            \/-(response.xml)
          }
          case Status.BAD_REQUEST => {
            logger.info("Bad Request")
            -\/("")
          }
          case Status.FORBIDDEN => {
            logger.info("forbidden")
            -\/("")
          }
          case Status.SERVICE_UNAVAILABLE => {
            logger.info("Service unavailable")
            -\/("")
          }
          case Status.GATEWAY_TIMEOUT => {
            logger.info("Gateway Timeout")
            -\/("")
          }
          case Status.INTERNAL_SERVER_ERROR => -\/("")
          case _ => {
            logger.info("Internal server error")
            -\/("")
          }
        }
    }.recover {
      case e =>
        -\/("")
    }
  }

}
