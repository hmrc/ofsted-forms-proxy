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

import java.time.LocalDateTime

import play.api.libs.json._
import uk.gov.hmrc.ofstedformsproxy.json.ValueClassFormat

case class OfstedNotification(
                               formId: FormId,
                               formLink: FormLink,
                               time: LocalDateTime = LocalDateTime.now,
                               emailAddress: EmailAddress = EmailAddress(""),
                               kind: String = "")

case class EmailAddress(value: String)

object EmailAddress {
  implicit val format: OFormat[EmailAddress] = ValueClassFormat.oformat("email", EmailAddress.apply, _.value)

  val vformat: Format[EmailAddress] = ValueClassFormat.vformat("email", EmailAddress.apply, x => JsString(x.value))
}


case class TemplateId(value: String)

object TemplateId {
  implicit val format: OFormat[TemplateId] = ValueClassFormat.oformat("templateId", TemplateId.apply, _.value)

  val vformat: Format[TemplateId] = ValueClassFormat.vformat("templateId", TemplateId.apply, x => JsString(x.value))
}


case class FormLink(link: String)
