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
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.ofstedformsproxy.config.AppConfig
import uk.gov.hmrc.ofstedformsproxy.http.HttpProxyClient
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CygnumConnectorImpl])
trait CygnumConnector {

  def send()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse]
}

@Singleton
class CygnumConnectorImpl @Inject() (auditConnector:    AuditConnector,
                                     metrics:           Metrics,
                                     wsClient:          WSClient,
                                     system:            ActorSystem)(
                                     appConfig: AppConfig) extends CygnumConnector {
                                      //TODO: Check if we need LogMessageTransformer
                                      //TODO: Add Logging

  val logger : Logger = Logger(this.getClass())

  val proxyClient: HttpProxyClient = new HttpProxyClient(auditConnector, appConfig.runModeConfiguration, wsClient, "microservice.services.cygnum.proxy")

  //TODO: Get the URL to call from config
  private val cygnumUrl = "https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc"

  override def send()(implicit hc : HeaderCarrier, ec : ExecutionContext) = {
    proxyClient.post(cygnumUrl, "", Map("Content-Type" -> "application/soap+xml; charset=utf-8"))
  }

}
