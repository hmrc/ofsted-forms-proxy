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

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}
import play.api.i18n._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import scalaz.{-\/, \/, \/-}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.ofstedformsproxy.connectors.{CygnumConnector, OutboundServiceConnector}
import uk.gov.hmrc.ofstedformsproxy.logging.{LoggingHelper, NotificationLogger}
import uk.gov.hmrc.ofstedformsproxy.models.OutboundCallRequest
import uk.gov.hmrc.ofstedformsproxy.service.{AuditingService, SOAPMessageService}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import java.net.URL

import scala.xml.Elem

@Singleton
class OutboundCallController @Inject()(outboundServiceConnector: OutboundServiceConnector,
                                       soapService: SOAPMessageService,
                                       cc: CygnumConnector,
                                       logger: NotificationLogger,
                                       messagesApi: MessagesApi,
                                       auditingService: AuditingService)
  extends CygnumController(outboundServiceConnector, logger, messagesApi) {

  def submit2() = Action.async {
    implicit request =>

      val payload: String \/ String = cc.getUrn()

      payload match {
        case \/-(s) => callOutboundService(OutboundCallRequest(new URL("https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc"), "", "", Seq.empty, s))
        case -\/(e) => Future.successful(BadRequest(e))
      }

  }

  def submit(): Action[Try[JsValue]] = validateAccept(acceptHeaderValidation).async(tryJsonParser) {
    implicit request =>

      request.body match {

        case Success(js) =>
          js.validate[OutboundCallRequest] match {
            case JsSuccess(requestPayload, _) =>
              logger.debug(s"${LoggingHelper.logMsgPrefix(requestPayload.conversationId)} Notification passed header validation with payload containing ", url = requestPayload.url.toString, payload = requestPayload.xmlPayload)
              callOutboundService(requestPayload)
            case error: JsError =>
              logger.error("JSON payload failed schema validation")
              Future.successful(invalidJsonErrorResponse(error).JsonResult)
          }

        case Failure(ex) =>
          logger.error(nonJsonBodyErrorMessage)
          Future.successful(errorBadRequest(nonJsonBodyErrorMessage).JsonResult)
      }
  }

  def callOutboundService(outboundCallRequest: OutboundCallRequest)(implicit hc: HeaderCarrier): Future[Result] = {
    val logPrefix = LoggingHelper.logMsgPrefix(outboundCallRequest.conversationId)
    val startTime = LocalDateTime.now
    outboundServiceConnector.callOutboundService(outboundCallRequest)
      .map { response =>
        logCallDuration(startTime, logPrefix)
        logger.info(s"${logPrefix}Outbound call succeeded")
        auditingService.auditSuccessfulNotification(outboundCallRequest)
        println(response.body)
        val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
        val tmp = (xmlResponse \\ "GetDataResult").text
        val urn = (scala.xml.XML.loadString(tmp) \\ "URN").text
        Ok(Json.obj("urn" -> urn))
//        NoContent
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

  protected def logCallDuration(startTime: LocalDateTime, logPrefix: String)(implicit hc: HeaderCarrier): Unit ={
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"${logPrefix}Outbound call duration was ${callDuration} ms")
  }


  private def recovery(outboundCallRequest: OutboundCallRequest, logMsg: String, failureReason: String, thr: Throwable)(implicit hc: HeaderCarrier) = {
    logger.error(logMsg, thr)
    auditingService.auditFailedNotification(request = outboundCallRequest, Some(failureReason))
    CustomErrorResponses.badGatewayErrorResponses.JsonResult
  }

}
