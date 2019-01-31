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


import com.google.inject.Inject
import javax.inject.Singleton
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.ofstedformsproxy.logging.NotificationLogger
import uk.gov.hmrc.ofstedformsproxy.models.OutboundCallRequest
import uk.gov.hmrc.ofstedformsproxy.service.WSProxyPost

import scala.concurrent.Future

@Singleton
class OutboundServiceConnector @Inject()(outboundProxy: WSProxyPost, logger: NotificationLogger) extends BaseConnector(outboundProxy, logger) {

  def callOutboundService(request: OutboundCallRequest): Future[HttpResponse] = {
    val urlString = request.url.toString

    val headers = Seq((CONTENT_TYPE, "application/soap+xml; charset=utf-8")) ++
      request.outboundCallHeaders.map(h => (h.name, h.value))

    logger.debug(s"Calling external service $urlString", headers)
    doPost(urlString, headers, request.xmlPayload)
  }

}
