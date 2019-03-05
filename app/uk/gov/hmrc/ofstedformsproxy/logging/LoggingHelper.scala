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

package uk.gov.hmrc.ofstedformsproxy.logging

import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.models.SeqOfHeader

object LoggingHelper {

  private val headerOverwriteValue = "value-not-logged"
  private val headersToOverwrite = Set(AUTHORIZATION)

  def formatError(msg: String)(implicit hc: HeaderCarrier): String = {
    formatInfo(msg)
  }

  def formatInfo(msg: String)(implicit hc: HeaderCarrier): String = {
    val headers = hc.headers
    formatInfo(msg, headers)
  }

  def formatInfo(msg: String, headers: SeqOfHeader): String = {
    s"${headers.toString} $msg"
  }

  def formatDebug(msg: String, headers: SeqOfHeader): String = {
    s"${headers.toString} $msg\nheaders=${overwriteHeaderValues(headers, headersToOverwrite - AUTHORIZATION)}"
  }

  def formatDebug(msg: String, maybeUrl: Option[String] = None, maybePayload: Option[String] = None)(implicit hc: HeaderCarrier): String = {
    val headers: Seq[(String, String)] = hc.headers
    val urlPart = maybeUrl.fold("")(url => s" url=$url")
    val payloadPart = maybePayload.fold("")(payload => s"\npayload=\n$payload")
    s"${headers.toString} $msg$urlPart\nheaders=${overwriteHeaderValues(headers, headersToOverwrite - AUTHORIZATION)}$payloadPart"
  }

  private def findHeaderValue(headerName: String, headers: SeqOfHeader): Option[String] = {
    headers.collectFirst {
      case header if header._1.equalsIgnoreCase(headerName) => header._2
    }
  }

  private def overwriteHeaderValues(headers: SeqOfHeader, overwrittenHeaderNames: Set[String]): SeqOfHeader = {
    headers map {
      case (rewriteHeader, _) if overwrittenHeaderNames.contains(rewriteHeader) => rewriteHeader -> headerOverwriteValue
      case header => header
    }
  }

}
