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

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.ofstedformsproxy.handlers.FormSubmissionResponseHandler._

import scala.io.Source
import scala.xml.NodeSeq

class FormSubmissionResponseHandlerSpec extends WordSpec with MustMatchers {

  "process form success submission response" in {
    val successResponse = cygnumResponse("./test/uk/gov/hmrc/ofstedformsproxy/cygnum_example_response/success_response.xml")
    val expectedResponse = CygnumResponse(OkStatus, sendDataResult(successResponse).get)

    processFormSubmissionResponse(successResponse) mustBe expectedResponse
  }

  "process form wrong format submission response" in {
    val wrongFormatResponse = cygnumResponse("./test/uk/gov/hmrc/ofstedformsproxy/cygnum_example_response/wrong_format_missing_<Data>_elm_response.xml")
    val expectedResponse = CygnumResponse(BadStatus, sendDataResult(wrongFormatResponse).get)

    processFormSubmissionResponse(wrongFormatResponse) mustBe expectedResponse
  }

  "process empty status submission response" in {
    val emptyStatusResponse =
      """<SendDataResponse>
            <SendDataResult></SendDataResult>
        </SendDataResponse>""".stripMargin

    processFormSubmissionResponse(emptyStatusResponse) must matchPattern {
      case CygnumResponse(BadStatus, NodeSeq.Empty) =>
    }
  }


  val cygnumResponse: String => String =
    fileLocation => Source.fromFile(fileLocation).getLines.mkString
}
