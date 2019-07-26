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

package uk.gov.hmrc.ofstedformsproxy.handlers

import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

object FormSubmissionResponseHandler {

  def processFormSubmissionResponse(response: String): CygnumResponse = {
    val responseStatus: Option[CygnumResponse] = (for {
      unescapedDataResult <- sendDataResult(response)
      status <- Try((unescapedDataResult \\ "Status").text)
      cygnumStatus = if (status == "0") OkStatus else BadStatus
    } yield CygnumResponse(cygnumStatus, unescapedDataResult)).toOption

    responseStatus.fold(CygnumResponse(BadStatus, NodeSeq.Empty))(res => res)
  }

  def sendDataResult(response: String): Try[Elem] =
    for {
      xmlResponse <- Try(XML.loadString(response))
      dataResult <- Try((xmlResponse \\ "SendDataResult").text)
      unescapedDataResult <- Try(XML.loadString(dataResult))
    } yield unescapedDataResult
}
