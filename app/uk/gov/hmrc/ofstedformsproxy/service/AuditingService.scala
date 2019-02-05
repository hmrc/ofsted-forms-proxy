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

package uk.gov.hmrc.ofstedformsproxy.service
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.customs.api.common.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.logging.{LoggingHelper, NotificationLogger}
import uk.gov.hmrc.ofstedformsproxy.models.OutboundCallRequest
import uk.gov.hmrc.play.audit.EventKeys.TransactionName
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@Singleton
class AuditingService @Inject()(logger: NotificationLogger, servicesConfig: ServicesConfig, auditConnector: AuditConnector) {

  private val appName = "ofsted-forms-proxy"
  private val transactionNameValue = "ofsted-forms-proxy-outbound-call"
  private val ofstedFormsProxyOutboundCall = "OfstedFormsProxyOutboundCall"
  private val outboundCallUrl = "outboundCallUrl"
  private val outboundCallAuthToken = "outboundCallAuthToken"
  private val xConversationId = "x-conversation-id"
  private val result = "result"
  private val generatedAt = "generatedAt"

  private val failureReasonKey = "failureReason"

  def auditFailedNotification(request: OutboundCallRequest, failureReason: Option[String]): Unit = {
    auditNotification(request, "FAILURE", failureReason)
  }

  def auditSuccessfulNotification(request: OutboundCallRequest): Unit = {
    auditNotification(request, "SUCCESS", None)
  }

  private def auditNotification(request: OutboundCallRequest, successOrFailure: String, failureReason: Option[String]): Unit = {

    implicit val carrier = HeaderCarrier()

    val tags = Map(TransactionName -> transactionNameValue)

    val detail: JsObject = failureReason.fold(
      JsObject(Map[String, JsValue](
        outboundCallUrl -> JsString(request.url.toString),
        outboundCallAuthToken -> JsString(request.authHeaderToken),
        result -> JsString(successOrFailure),
        generatedAt -> JsString(DateTimeUtils.now.toString)
      )))(reason => {
      JsObject(Map[String, JsValue](
        outboundCallUrl -> JsString(request.url.toString),
        outboundCallAuthToken -> JsString(request.authHeaderToken),
        result -> JsString(successOrFailure),
        generatedAt -> JsString(DateTimeUtils.now.toString),
        failureReasonKey -> JsString(reason)
      ))
    }
    )

    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = appName,
        auditType = ofstedFormsProxyOutboundCall,
        tags = tags,
        detail = detail
      )).onComplete {
      case Success(auditResult) =>
        logger.info(s"Successfully audited $successOrFailure event")
        logger.debug(
          s"""Successfully audited $successOrFailure event with
             |payload url=${request.url}
             |payload headers=${request.outboundCallHeaders}
             |audit response=$auditResult""".stripMargin, Seq())
      case Failure(ex) =>
        logger.error(s"Failed to audit $successOrFailure event", ex)
    }
  }
}
