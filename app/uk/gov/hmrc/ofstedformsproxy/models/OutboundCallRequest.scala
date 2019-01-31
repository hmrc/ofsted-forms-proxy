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

package uk.gov.hmrc.ofstedformsproxy.models

import java.net.URL

import play.api.libs.json.Reads._
import play.api.libs.json._


import java.net.URL

import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.util._

case class OutboundCallRequest(url: URL,
                               conversationId: String,
                               authHeaderToken: String,
                               outboundCallHeaders: Seq[Header],
                               xmlPayload: String)

object OutboundCallRequest {

  private implicit val urlFormats = Format[URL](
    Reads { js =>
      lazy val supportedProtocols = Seq("http", "https")
      js.validate[String].map(s => Try(new URL(s))).flatMap {
        case Success(url) if supportedProtocols.contains(url.getProtocol) => JsSuccess(url)
        case Success(url) => JsError("unsupported protocol: " + url.getProtocol)
        case Failure(e) => JsError(e.getMessage)
      }
    },
    Writes(url => JsString(url.toString))
  )

  private implicit val headerFormats = Json.format[Header]

  implicit val formats: OFormat[OutboundCallRequest] = Json.format[OutboundCallRequest]
}
