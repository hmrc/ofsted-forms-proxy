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

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import scalaz.{-\/, \/-}
import uk.gov.hmrc.ofstedformsproxy.connectors.CygnumConnector
import uk.gov.hmrc.ofstedformsproxy.models.{SubmissionFailure, SubmissionSuccess}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

//TODO: do not inject the connector here, inject the service class
@Singleton
class OfstedFormsProxyController @Inject()(val messagesApi: MessagesApi, cc: CygnumConnector) extends FrontendController with I18nSupport {

  implicit val ofstedCharset = Codec.utf_8

  //TODO: need to handle different forms
  //TODO: Make request and evaluate response
  //TODO: Add the AuthModule ???
  //TODO: Play Crypto Secret
  //TODO: Add Prod Router
  //TODO: Set up logger config
  //TODO: Generate the digest values
  //TODO: Generate the timestamp values
  //TODO: set up kibana metrics
  //TODO: add cygnum connection configuration
  //TODO: set up routes
  //TODO: set up service manager configuration
  //TODO: set up app-base-config
  //TODO: set up app-common-config
  //TODO: set up app-development-config
  //TODO: set up app-staging-config
  //TODO: set up app-qa-config
  //TODO: set up app-production-config
  //TODO: build pipeline
  //TODO: Squid integration
  //TODO: Construct SOAP Envelope
  //TODO: Construct XML request
  //TODO: handle the response here
  //TODO: get the SOAP body
  //TODO: build the soap envelope
  //TODO: build the body
  //TODO: make the request
  //TODO: process response
  //TODO: extract the id and send back as a JSON value

  def getUrn() = Action { implicit request =>

    cc.getUrn() match {
      case \/-(u) => Ok(u.get.value)
      case -\/(e) => InternalServerError(e)
    }

  }

  def send(formId: String) = Action.async {
    implicit request =>
      cc.send().map {
        case \/-(SubmissionSuccess(_)) => Ok("")
        case -\/(SubmissionFailure(_)) => Ok("") //what status do I return here?
      }
  }

}
