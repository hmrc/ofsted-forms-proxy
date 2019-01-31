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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.ofstedformsproxy.logging.NotificationLogger

import scala.concurrent.Future

trait HeaderValidator extends Results {

  private lazy val validAcceptHeaders = Seq("application/soap+xml; charset=utf-8")

  val acceptHeaderValidation: (Option[String] => Boolean) = _ exists (validAcceptHeaders.contains(_))

  val notificationLogger: NotificationLogger

  def validateAccept(rules: Option[String] => Boolean): ActionBuilder[Request] = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      val logMessage = "Received notification"
      val headers = request.headers.headers
      notificationLogger.debug(logMessage, headers)

      val acceptHeader = HeaderNames.ACCEPT
      val hasAccept = rules(request.headers.get(acceptHeader))

      if (hasAccept) {
        notificationLogger.debug(s"$acceptHeader header passed validation", headers)
        block(request)
      } else {
        notificationLogger.debug(s"$acceptHeader header failed validation", headers)
        Future.successful(ErrorAcceptHeaderInvalid.JsonResult)
      }
    }
  }
}
