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

package uk.gov.hmrc.ofstedformsproxy.notification

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.ofstedformsproxy.handlers.NotifyRequest

class NotifyRequestSpec extends WordSpec with MustMatchers {

  "can be parsed from/to json" in {
    val email = EmailAddress("mikey@mouse")
    val request = NotifyRequest(TemplateId("333"), email, Map("firstName" -> "Tom", "lastName" -> "Cruise"))

    Json.parse("""{"templateId":"333","email":"mikey@mouse", "properties": {"firstName": "Tom","lastName": "Cruise"}}""")
      .as[NotifyRequest] mustBe request
  }
}
