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

import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorContentTypeHeaderInvalid
import uk.gov.hmrc.ofstedformsproxy.logging.OfstedFormProxyLogger

import scala.concurrent.Future

trait HeaderValidator extends Results {

  private lazy val validContentTypes = Seq("application/xml")

  val contentTypeValidation: (Option[String] => Boolean) = _ exists (validContentTypes.contains(_))

  val notificationLogger: OfstedFormProxyLogger

  def validateAccept(rules: Option[String] => Boolean): ActionBuilder[Request] = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      val logMessage = "Received payload"
      val headers = request.headers.headers
      notificationLogger.debug(logMessage, headers)

      val contentTypeHeader = HeaderNames.CONTENT_TYPE
      val hasContentType = rules(request.headers.get(contentTypeHeader))

      if (hasContentType) {
        notificationLogger.debug(s"$contentTypeHeader header passed validation", headers)
        block(request)
      } else {
        notificationLogger.debug(s"$contentTypeHeader header failed validation", headers)
        Future.successful(ErrorContentTypeHeaderInvalid.JsonResult)
      }
    }
  }
}
