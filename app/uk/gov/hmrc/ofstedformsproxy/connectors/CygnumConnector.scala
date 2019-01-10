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

package uk.gov.hmrc.ofstedformsproxy.connectors

import akka.actor.ActorSystem
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.Logger
import play.api.http.{HttpVerbs, Status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.RunMode
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.http.HttpProxyClient
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

trait CygnumConnector {

  def logger : Logger

 // def soapClient : SoapPost
//
//  private def soapHeaders() : Seq[(String, String)] = Seq(
//    CONTENT_TYPE  -> "application/soap+xml; charset=utf-8"
//  )

  //private def postRequest(url : String, headers : Seq[(String, String)], soapBody : String, soapAction : Option[String] = None)(implicit hc : HeaderCarrier) : Future[SoapHttpResponse] = ???

  //def send(payload : String)(implicit hc : HeaderCarrier, ex: ExecutionContext) : Result
}

class CygnumConnectorImpl @Inject() (auditConnector: AuditConnector,
                                     metrics : Metrics,
                                     wsClient : WSClient,
                                     system : ActorSystem)(appConfig : AppConfig) extends CygnumConnector {

  val proxyClient: HttpProxyClient = new HttpProxyClient(auditConnector, appConfig.runModeConfiguration, wsClient, "microservice.services.cygnum.proxy")

//  def send(payload : String)(implicit hc : HeaderCarrier, ec : ExecutionContext) = {
//    proxyClient.post("ofsted url", "the fiddle file, unformatted, and utf-8", Map())
//  }

  override def logger: Logger = Logger(this.getClass())

}