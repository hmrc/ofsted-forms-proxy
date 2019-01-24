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

import play.api.libs.json.Json
import scalaz.Monoid

case class SoapMessageException(message: String)

object SoapMessageException {

  implicit object SoapMessageExceptionMonoid extends Monoid[SoapMessageException] {
    def zero: SoapMessageException = SoapMessageException("")

    def append(s1: SoapMessageException, s2: => SoapMessageException): SoapMessageException = SoapMessageException(s1.message + " | " + s2.message)
  }

  implicit val soapExceptionFormat = Json.format[SoapMessageException]
}
