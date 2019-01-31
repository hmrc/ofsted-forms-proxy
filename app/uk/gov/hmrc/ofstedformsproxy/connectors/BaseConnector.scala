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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.ofstedformsproxy.logging.NotificationLogger
import uk.gov.hmrc.ofstedformsproxy.service.WSProxyPost

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseConnector @Inject()(outboundProxy: WSProxyPost, logger: NotificationLogger) {

  protected lazy implicit val emptyHC: HeaderCarrier = HeaderCarrier()

  protected def doPost(urlString: String, headers: Seq[(String, String)], payload: String): Future[HttpResponse] = {
    logger.debug(s"Calling external \nurl=\n$urlString \npayload=\n$payload", headers)

    val result = outboundProxy.POSTString(urlString, payload, headers)
    result.onComplete {
      case Success(response) =>
        logger.debug(s"Successful POST to external service $urlString and received response status=${response.status} and \npayload=\n${response.body}", headers)
      case Failure(e) => logger.error(s"Failed POST to $urlString", e)
    }
    result
  }

}
