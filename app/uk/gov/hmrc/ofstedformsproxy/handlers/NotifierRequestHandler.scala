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

import cats.{Applicative, Monad, MonadError}
import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.ofstedformsproxy.notification.{EmailAddress, OfstedNotificationClient, OfstedNotificationClientResponse, TemplateId}

class NotifierRequestHandler[F[_]: Monad](notificationClient: OfstedNotificationClient[F]) {

  def handleRequest(notifyRequest: NotifyRequest)(implicit M: Applicative[F], me: MonadError[F, String]): F[Response] = {
    notificationClient.send(notifyRequest).map {
      case OfstedNotificationClientResponse(_) => Response(200, "")
      case _ => Response(200, "")
    }
  }
}

case class Response(status: Int, msg: String)

case class NotifyRequest(templateId: TemplateId, email: EmailAddress, properties: Map[String, String])

object NotifyRequest {

  implicit val format: OFormat[NotifyRequest] = Json.format[NotifyRequest]
}
