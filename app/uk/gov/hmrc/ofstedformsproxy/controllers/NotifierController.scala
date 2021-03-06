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
import play.api.mvc.Action
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.handlers.{NotifierRequestHandler, NotifyRequest, _}
import uk.gov.hmrc.ofstedformsproxy.logging.OfstedFormProxyLogger
import uk.gov.hmrc.ofstedformsproxy.notification.{Notifier, OfstedNotificationClient}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

//TODO remove Guice!
class NotifierController @Inject()(config: AppConfig, logger: OfstedFormProxyLogger)(implicit ex: ExecutionContext) extends BaseController {

  def sendNotification(): Action[NotifyRequest] = Action.async(parse.json[NotifyRequest]) {
    implicit request =>
      logger.info(s"Notification request received ${request.body}")
      new NotifierRequestHandler[Try](new OfstedNotificationClient[Try](new Notifier[Try] {})).handleRequest(request.body) match {
        case Success(value) if value.status == 200 =>
          logger.info(s"Response with status ${value.status} body ${value.msg}")
          Future.successful(Ok(""))
        case Success(value) =>
          logger.info(s"Response with ${value.status}")
          Future.successful(BadRequest(value.msg))
        case Failure(e) =>
          logger.error(s"Response error with ${e.getMessage}")
          Future.failed(e)
      }

  }
}

