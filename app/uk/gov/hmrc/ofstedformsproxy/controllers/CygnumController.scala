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

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.BodyParser

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.connectors.BaseConnector
import uk.gov.hmrc.ofstedformsproxy.logging.OfstedFormProxyLogger
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

abstract class CygnumController @Inject() (baseConnector: BaseConnector,
                                           logger: OfstedFormProxyLogger,
                                           val messagesApi: MessagesApi)
  extends BaseController with I18nSupport with HeaderValidator {

  override val notificationLogger: OfstedFormProxyLogger = logger
  protected val nonJsonBodyErrorMessage = "Request does not contain a valid JSON body"
  protected def tryJsonParser: BodyParser[Try[JsValue]] = parse.tolerantText.map(text => Try(Json.parse(text)))

  protected def invalidJsonErrorResponse(jsError: JsError)(implicit messages: Messages, hc: HeaderCarrier) = {
    val contents = for {
      (jsPath, validationErrors) <- jsError.errors
      validationError <- validationErrors
      errorMessage = s"$jsPath: ${messages(validationError.message, validationError.args: _*)}"
    } yield ResponseContents("INVALID_JSON", errorMessage)
    logger.error("failed JSON schema validation")

    //errorBadRequest("Request failed schema validation").withErrors(contents: _*)
  }

}
