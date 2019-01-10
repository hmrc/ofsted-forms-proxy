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

package uk.gov.hmrc.ofstedformsproxy.http

import play.api.Configuration
import play.api.http.HttpVerbs
import play.api.libs.json.Writes
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws.{WSProxy, WSProxyConfiguration}

import scala.concurrent.{ExecutionContext, Future}

class HttpProxyClient(override val auditConnector: AuditConnector, config: Configuration, wsClient: WSClient, proxyConfigPath: String)
  extends DefaultHttpClient(config, auditConnector, wsClient) with WSProxy with HttpVerbs {

  override lazy val wsProxyServer = WSProxyConfiguration(proxyConfigPath)

  private class RawHttpReads extends HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  private val rawHttpReads = new RawHttpReads

  def post[A](url: String,
              body: A,
              headers: Map[String, String] = Map.empty[String, String]
             )(implicit w: Writes[A], hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    POST(url, body, headers.toSeq)(w, rawHttpReads, hc, ec)
}
