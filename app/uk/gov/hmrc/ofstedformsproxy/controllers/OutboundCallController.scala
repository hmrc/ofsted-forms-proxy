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

package uk.gov.hmrc.ofstedformsproxy.controllers

import java.net.URL
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import scalaz.{-\/, \/-}
import uk.gov.hmrc.http._
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.connectors.{CygnumConnector, OutboundServiceConnector}
import uk.gov.hmrc.ofstedformsproxy.logging.{LoggingHelper, NotificationLogger}
import uk.gov.hmrc.ofstedformsproxy.models.OutboundCallRequest
import uk.gov.hmrc.ofstedformsproxy.service.{AuditingService, SOAPMessageService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.Elem

@Singleton
class OutboundCallController @Inject()(outboundServiceConnector: OutboundServiceConnector,
                                       soapService: SOAPMessageService,
                                       cc: CygnumConnector,
                                       logger: NotificationLogger,
                                       messagesApi: MessagesApi,
                                       auditingService: AuditingService,
                                       appConfig: AppConfig)
  extends CygnumController(outboundServiceConnector, logger, messagesApi) {


  def submitForm() = Action.async {
    implicit request =>

      val payload = request.body.asXml

      payload match {
        case Some(p) => {
          soapService.buildFormSubmissionSOAPPayload(p) match {
            case \/-(formPayload) => {
              logger.debug(s"Constructed Send Data payload: ", url = appConfig.cygnumURL, payload = p.toString)
              callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", "", Seq.empty, formPayload), processFormSubmissionResponse)
            }
            case -\/(error) => {
              logger.error("Failed to build the Send Data SOAP Payload")
              Future.successful(BadRequest(error))
            }
          }
        }
        case None => Future.successful(Ok(""))
      }
  }

  def getUrn() = Action.async {
    implicit request =>
      soapService.buildGetUrnSOAPPayload() match {
        case \/-(getUrnPayload) => {
          logger.debug(s"Constructed GetURN payload: ", url = appConfig.cygnumURL, payload = getUrnPayload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", "", Seq.empty, getUrnPayload), processGetURNResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetURN SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  private def processGetURNResponse(response: HttpResponse) = {
    val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
    val tmp: String = (xmlResponse \\ "GetDataResult").text
    val urn: String = (scala.xml.XML.loadString(tmp) \\ "URN").text

    if (!urn.isEmpty)
      Ok(Json.obj("urn" -> urn))
    else
      Conflict
  }

  private def processFormSubmissionResponse(response: HttpResponse) = {
    val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
    val tmp: String = (xmlResponse \\ "SendDataResult").text
    val status: String = (scala.xml.XML.loadString(tmp) \\ "Status").text

    if (!status.isEmpty)
      Ok(Json.obj("status" -> status))
    else
      Conflict
  }

  private def callOutboundService(outboundCallRequest: OutboundCallRequest, responseHandler: HttpResponse => Result)(implicit hc: HeaderCarrier): Future[Result] = {
    val logPrefix = LoggingHelper.logMsgPrefix(outboundCallRequest.conversationId)
    val startTime = LocalDateTime.now
    outboundServiceConnector.callOutboundService(outboundCallRequest)
      .map { response =>
        logCallDuration(startTime, logPrefix)
        logger.info(s"${logPrefix}Outbound call succeeded")
        auditingService.auditSuccessfulNotification(outboundCallRequest)
        responseHandler(response)
      }.recover {
      case upstream4xx: Upstream4xxResponse =>
        logCallDuration(startTime, logPrefix)
        val logMsg = s"${logPrefix}Outbound call failed with Upstream4xxResponse status=${upstream4xx.upstreamResponseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${upstream4xx.upstreamResponseCode.toString}", upstream4xx)
      case upstream5xx: Upstream5xxResponse =>
        logCallDuration(startTime, logPrefix)
        val logMsg = s"${logPrefix}Outbound call failed with Upstream5xxResponse status=${upstream5xx.upstreamResponseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${upstream5xx.upstreamResponseCode.toString}", upstream5xx)
      case httpException: HttpException =>
        logCallDuration(startTime, logPrefix)
        val logMsg = s"${logPrefix}Outbound call failed with response status=${httpException.responseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${httpException.responseCode.toString}", httpException)
      case NonFatal(thr) =>
        logCallDuration(startTime, logPrefix)
        val logMsg = s"${logPrefix}Outbound call failed due to ${thr.getMessage}"
        recovery(outboundCallRequest, logMsg, "http status unknown", thr)
    }
  }

  protected def logCallDuration(startTime: LocalDateTime, logPrefix: String)(implicit hc: HeaderCarrier): Unit = {
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"${logPrefix}Outbound call duration was ${callDuration} ms")
  }


  private def recovery(outboundCallRequest: OutboundCallRequest, logMsg: String, failureReason: String, thr: Throwable)(implicit hc: HeaderCarrier) = {
    logger.error(logMsg, thr)
    auditingService.auditFailedNotification(request = outboundCallRequest, Some(failureReason))
    CustomErrorResponses.badGatewayErrorResponses.JsonResult
  }

}
