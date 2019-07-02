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

import uk.gov.service.notify.NotificationClient
import pureconfig.generic.auto._


trait OfstedNotificationConf {
  val ofstedNotification: OfstedNotificationConfig =
    pureconfig.loadConfigOrThrow[OfstedNotificationConfig]("ofsted.notifications")
  val notificationClient: NotificationClient = new NotificationClient(ofstedNotification.apiKey)

  private val formTemplatesId: Map[String, String] = ofstedNotification.templates

  val formTemplates: Map[FormStatus, String] = formTemplatesId map {
    case ("submitted", v) => Submitted  -> v
    case ("rejected", v)  => InProgress -> v
    case ("accepted", v)  => Approved   -> v
  }
}


case class OfstedNotificationConfig(
                                     apiKey: String,
                                     templates: Map[String, String],
                                     email: String,
                                     formLinkPrefix: String)
