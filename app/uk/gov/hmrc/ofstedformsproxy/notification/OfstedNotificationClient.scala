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

import cats.implicits._
import cats.{Applicative, MonadError}
import uk.gov.hmrc.ofstedformsproxy.handlers.NotifyRequest
import uk.gov.service.notify.SendEmailResponse

import scala.collection.JavaConverters._

trait Notifier[F[_]] extends OfstedNotificationConf {

  def notifyByEmail(notifyRequest: NotifyRequest)(
    implicit me: MonadError[F, String], M: Applicative[F]): F[SendEmailResponse] = {
    import notifyRequest._
    val sendEmailResponse = notificationClient.sendEmail(templateId.value, email.value, properties.asJava, "")
    M.pure(sendEmailResponse)
  }

}

class OfstedNotificationClient[F[_]](notifier: Notifier[F]) extends FormLinkBuilder {

  def send(notifyRequest: NotifyRequest)(implicit me: MonadError[F, String]): F[OfstedNotificationClientResponse] =
      notifier.notifyByEmail(notifyRequest)
      .map(emailResponse => OfstedNotificationClientResponse(emailResponse))
}

case class OfstedNotificationClientResponse(emailResponse: SendEmailResponse)

trait FormLinkBuilder extends OfstedNotificationConf {
  def buildLink(formId: FormId): FormLink = {
    val link = s"${ofstedNotification.formLinkPrefix}${formId.value}"
    FormLink(link)
  }
}

//TODO the following is very likely to be removed
case class FormId(value: String)

