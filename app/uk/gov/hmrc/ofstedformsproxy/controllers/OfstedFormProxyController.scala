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

import cats.instances.int._
import cats.syntax.eq._
import java.net.URL
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}
import play.api.i18n._
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc._
import scalaz.{-\/, \/-}
import uk.gov.hmrc.http._
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.connectors.OutboundServiceConnector
import uk.gov.hmrc.ofstedformsproxy.handlers.{CygnumResponse, FormSubmissionResponseHandler, OkStatus}
import uk.gov.hmrc.ofstedformsproxy.logging.OfstedFormProxyLogger
import uk.gov.hmrc.ofstedformsproxy.models.OutboundCallRequest
import uk.gov.hmrc.ofstedformsproxy.service.{AuditingService, SOAPMessageService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.Elem

//  //TODO: Add the Auth action filter
//  //TODO: Play Crypto Secret
//  //TODO: Add Prod Router
//  //TODO: set up kibana metrics
//  //TODO: set up service manager configuration
//  //TODO: set up app-base-config
//  //TODO: set up app-common-config
//  //TODO: set up app-development-config
//  //TODO: set up app-staging-config
//  //TODO: set up app-qa-config
//  //TODO: set up app-production-config
//  //TODO: build pipeline

@Singleton
class OfstedFormProxyController @Inject()(outboundServiceConnector: OutboundServiceConnector,
                                          soapService: SOAPMessageService,
                                          logger: OfstedFormProxyLogger,
                                          val messagesApi: MessagesApi,
                                          auditingService: AuditingService,
                                          appConfig: AppConfig)
  extends BaseController with I18nSupport with HeaderValidator {

  def submitForm(): Action[AnyContent] = validateAccept(contentTypeValidation).async {
    implicit request =>
      request.body.asXml match {
        case Some(p) => {
          soapService.buildFormSubmissionPayload(p) match {
            case \/-(formPayload) => {
              logger.debug(s"Constructed Send Data payload: ", url = appConfig.cygnumURL, payload = p.toString)
              callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, formPayload), processFormSubmissionResponse)
            }
            case -\/(error) => {
              logger.error("Failed to build the form submission SOAP XML payload")
              Future.successful(BadRequest(error))
            }
          }
        }
        case None => Future.successful(BadRequest("Failed to parse XML payload"))
      }
  }

  def getUrn(): Action[AnyContent] = Action.async {
    implicit request =>
      soapService.buildGetURNPayload() match {
        case \/-(getUrnPayload) => {
          logger.debug(s"Constructed GetURN payload: ", url = appConfig.cygnumURL, payload = getUrnPayload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, getUrnPayload), processGetURNResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetURN SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  // Expects a JSON body of the form:
  // {
  //   "formId1": "ReferenceNumberType",
  //   "formId2": "ReferenceNumberType",
  //   "formId3": "ReferenceNumberType"
  //   ...
  // }
  // Where:
  //   "formIdn" is any identifier you like to associate with the returned URN for the form (e.g. SubmissionRef, or the FormId itself
  //  "ReferenceNumberType" is one of the reference number types defined in Common.xsd
  //
  // The response body will be of the form:
  // {
  //   "formId1": "URN1",
  //   "formId2": "URN2",
  //   "formId3": "URN3",
  //   ...
  // }
  // Where "URNn" is the new URN for each formIdn
  def getUrns(): Action[AnyContent] = Action.async {
    implicit request =>
      def reportProblemWithBody(body: String) = {
        val err = s"getUrns request body is invalid. Should be a JSON object containing fields whose names are some form of form id, and whose values are the ReferenceNumberType you want for the corresponding URN. Got $body"
        logger.error(err)
        Future.successful(BadRequest(err))
      }

      request.body.asJson match {
        case Some(obj: JsObject) =>
          val body = obj.fields.map { case (k, v) => (k, v.as[String]) }
          soapService.buildGetURNsPayload(body.map(_._2)) match {
            case \/-(getUrnsPayload) => {
              logger.debug(s"Constructed GetURNs payload: ", url = appConfig.cygnumURL, payload = getUrnsPayload)
              callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, getUrnsPayload), processGetURNsResponse(body.map(_._1)))
            }
            case -\/(error) => {
              logger.error("Failed to build the GetURNs SOAP Payload")
              Future.successful(BadRequest(error))
            }
          }
        case Some(v) => reportProblemWithBody(v.toString)
        case None => reportProblemWithBody("an empty body")
      }
  }

  def getIndividualDetails(individualId: String): Action[AnyContent] = Action.async {
    implicit request =>
      soapService.buildGetIndividualDetailsPayload(individualId) match {
        case \/-(payload) => {
          logger.debug(s"Constructed GetIndividualDetails payload: ", appConfig.cygnumURL, payload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, payload), processGetDataResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetIndividualDetails SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  def getRegistrationDetails(urn: String): Action[AnyContent] = Action.async {
    implicit request =>
      soapService.buildGetRegistrationDetailsPayload(urn) match {
        case \/-(payload) => {
          logger.debug(s"Constructed GetRegistrationDetails payload: ", appConfig.cygnumURL, payload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, payload), processGetDataResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetRegistrationDetails SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  def getOrganisationDetails(organisationId: String): Action[AnyContent] = Action.async {
    implicit request =>
      soapService.buildGetOrganisationDetailsPayload(organisationId) match {
        case \/-(payload) => {
          logger.debug(s"Constructed GetOrganisationDetails payload: ", appConfig.cygnumURL, payload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, payload), processGetDataResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetOrganisationDetails SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  def getELSProviderDetails(providerId: String): Action[AnyContent] = Action.async {
    implicit request =>
      soapService.buildGetELSProviderDetailsPayload(providerId) match {
        case \/-(payload) => {
          logger.debug(s"Constructed GetELSProvideDetails payload: ", appConfig.cygnumURL, payload)
          callOutboundService(OutboundCallRequest(new URL(appConfig.cygnumURL), "", Seq.empty, payload), processGetDataResponse)
        }
        case -\/(error) => {
          logger.error("Failed to build the GetELSProvideDetails SOAP Payload")
          Future.successful(BadRequest(error))
        }
      }
  }

  private def processGetURNResponse(response: HttpResponse)(implicit hc: HeaderCarrier): Result = {
    val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
    val tmp: String = (xmlResponse \\ "GetDataResult").text
    val urn: String = (scala.xml.XML.loadString(tmp) \\ "URN").text

    if (!urn.isEmpty) {
      logger.debug(s"Get Data service full response: ${xmlResponse.toString}: ", Seq.empty)
      Ok(Json.obj("urn" -> urn))
    }
    else {
      logger.error(s"Get Data service response: ${xmlResponse.toString}")
      BadRequest("Failed to get URN")
    }
  }

  private def processGetURNsResponse(keys: Seq[String])(response: HttpResponse)(implicit hc: HeaderCarrier): Result = {
    val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
    val tmp: String = (xmlResponse \\ "GetDataResult").text

    val urns = for {
      urnsNode <- (scala.xml.XML.loadString(tmp) \\ "URNs")
      urnNode <- urnsNode \\ "URN"
    } yield urnNode.text

    if (urns.size === keys.size) {
      logger.debug(s"Get Data service full response: ${xmlResponse.toString}: ", Seq.empty)
      Ok(JsObject(keys.zip(urns.map(JsString))))
    }
    else {
      logger.error(s"Get Data service response: ${xmlResponse.toString}")
      BadRequest("Failed to get URNs")
    }
  }

  private def processGetDataResponse(response: HttpResponse)(implicit hc: HeaderCarrier): Result = {
    val xmlResponse: Elem = scala.xml.XML.loadString(response.body)
    logger.debug(s"Get Data service full response: ${xmlResponse.toString}: ", Seq.empty)
    Ok(xmlResponse)
  }

  private def processFormSubmissionResponse(response: HttpResponse)(implicit hc: HeaderCarrier): Result =
    FormSubmissionResponseHandler.processFormSubmissionResponse(response.body) match {
      case res: CygnumResponse if res.status == OkStatus =>
        logger.debug(s"Send Data service full response: ${res.dataResults}: ", Seq.empty)
        Ok(Json.obj("status" -> res.status.toString))
      case res =>
        logger.error(s"Send Data service response: ${res.dataResults}")
        BadRequest(res.dataResults)
    }

  private def callOutboundService(outboundCallRequest: OutboundCallRequest, responseHandler: HttpResponse => Result)(implicit hc: HeaderCarrier): Future[Result] = {
    val startTime = LocalDateTime.now
    outboundServiceConnector.callOutboundService(outboundCallRequest)
      .map { response =>
        logCallDuration(startTime)
        logger.info(s"Outbound call succeeded")
        auditingService.auditSuccessfulNotification(outboundCallRequest)
        responseHandler(response)
      }.recover {
      case upstream4xx: Upstream4xxResponse =>
        logCallDuration(startTime)
        val logMsg = s"Outbound call failed with Upstream4xxResponse status=${upstream4xx.upstreamResponseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${upstream4xx.upstreamResponseCode.toString}", upstream4xx)
      case upstream5xx: Upstream5xxResponse =>
        logCallDuration(startTime)
        val logMsg = s"Outbound call failed with Upstream5xxRespons" +
          s"e status=${upstream5xx.upstreamResponseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${upstream5xx.upstreamResponseCode.toString}", upstream5xx)
      case httpException: HttpException =>
        logCallDuration(startTime)
        val logMsg = s"Outbound call failed with response status=${httpException.responseCode}"
        recovery(outboundCallRequest, logMsg, s"http status ${httpException.responseCode.toString}", httpException)
      case NonFatal(thr) =>
        logCallDuration(startTime)
        val logMsg = s"Outbound call failed due to ${thr.getMessage}"
        recovery(outboundCallRequest, logMsg, "http status unknown", thr)
    }
  }

  protected def logCallDuration(startTime: LocalDateTime)(implicit hc: HeaderCarrier): Unit = {
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Outbound call duration was ${callDuration} ms")
  }


  private def recovery(outboundCallRequest: OutboundCallRequest, logMsg: String, failureReason: String, thr: Throwable)(implicit hc: HeaderCarrier): Result = {
    logger.error(logMsg, thr)
    auditingService.auditFailedNotification(request = outboundCallRequest, Some(failureReason))
    CustomErrorResponses.badGatewayErrorResponses.JsonResult
  }

  override val notificationLogger: OfstedFormProxyLogger = logger

}
