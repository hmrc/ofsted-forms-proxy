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
import play.api.mvc._

import scala.concurrent.Future
import play.api.i18n.{I18nSupport, MessagesApi}
import play.filters.csrf.CSRFAddToken
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.connectors.CygnumConnector
import uk.gov.hmrc.ofstedformsproxy.views

//cygnumConnector : CygnumConnector)
@Singleton
class OfstedFormsProxyController @Inject()(val messagesApi: MessagesApi, cygnumConnector : CygnumConnector)  extends FrontendController with I18nSupport {

  implicit val ofstedCharset = Codec.utf_8

  //TODO: Keystore
  //TODO: Squid integration
  //TODO: Construct SOAP Envelope
  //TODO: Construct XML request
  //TODO: Make request and evaluate response
  //TODO: Add the CustomWsParser module
  //TODO: Add the AuthModule ???
  //TODO: Play Crypto Secret
  //TODO: Add Prod Router
  //TODO: Set up logger config
  //TODO: Generate the digest values
  //TODO: Generate the timestamp values
  //TODO: set up kibana metrics
  //TODO: add cygnum connection configuration
  //TODO: add squid configuration
  //TODO: set up routes

  //TODO: set up service manager configuration
  //TODO: set up app-base-config
  //TODO: set up app-common-config
  //TODO: set up app-development-config
  //TODO: set up app-staging-config
  //TODO: set up app-qa-config
  //TODO: set up app-production-config

  //TODO: build pipeline


  //TODO: need a base64 encoding of the public key for the certificate (who will do this and how do I add it) - get it from Ofsted!


  /*
    Get keystore and truststore setup and make a call using this setup plus the Fiddler.xml just to prove
    that the Play SSL lib can call the SOAP API like the Java client

    controller -> connector -> service -> service makes call

    controller should pass the header carrier object, and the payload (payload should be unformatted and with utf-8 encoding
   */

  def send = Action {
    implicit request =>
      /*
        Create the keystore/truststore // there should be no reference to these in the controller, they should be hidden in the service or http client
        Build the client
        Build the SOAP/XML HTTP request
        Make the request
       */

//      val payload = request.body.asXml
//
//      payload match {
//        case Some(_) => // make the request to cygnum
//        case None =>  // report bad request and log the problem
//      }
      cygnumConnector.send()
      Ok("")
  }

}
