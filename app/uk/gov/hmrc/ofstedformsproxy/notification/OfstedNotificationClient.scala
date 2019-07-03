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
import cats.{Monad, MonadError}
import uk.gov.hmrc.ofstedformsproxy.handlers.NotifyRequest
import uk.gov.service.notify.SendEmailResponse

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait Notifier[F[_]] extends OfstedNotificationConf {

  def notifyByEmail(templateId: String, emailAddress: EmailAddress, personalisation: Map[String, String])(
    implicit me: MonadError[F, String]): F[SendEmailResponse] =
    runNotification(Try(notificationClient.sendEmail(templateId, emailAddress.value, personalisation.asJava, "")))

  private def runNotification[T](fn: Try[T])(implicit me: MonadError[F, String]): F[T] = fn match {
    case Success(response) => me.pure(response)
    case Failure(ex)       => me.raiseError(s"Unable to notify reviewer ${ex.getMessage}")
  }
}

//TODO replace hard coded with personilised data Map
class OfstedNotificationClient[F[_]: Monad](notifier: Notifier[F]) extends FormLinkBuilder {

  def send(notifyRequest: NotifyRequest)(implicit me: MonadError[F, String]): F[OfstedNotificationClientResponse] =
    notifier
      .notifyByEmail(
        notifyRequest.templateId.value,
        notifyRequest.email,
        basicTemplate("to do", "to do", "to do"))
      .map(emailResponse => OfstedNotificationClientResponse(emailResponse))

  val basicTemplate: (String, String, String) => Map[String, String] =
    (formId, firstName, lastName) => Map("formId" -> formId, "firstName" -> firstName, "lastName" -> lastName)
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

