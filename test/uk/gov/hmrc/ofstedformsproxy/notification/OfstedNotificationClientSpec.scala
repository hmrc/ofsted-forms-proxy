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

import cats.{Id, MonadError}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.ofstedformsproxy.handlers.NotifyRequest
import uk.gov.service.notify.SendEmailResponse

class OfstedNotificationClientSpec extends WordSpec with MustMatchers with MockFactory {

  implicit val me = new MonadError[Id, String] {
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    override def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = ???
    override def raiseError[A](e: String): Id[A] = e.asInstanceOf[A]
    override def handleErrorWith[A](fa: Id[A])(f: String => Id[A]): Id[A] = ???
    override def pure[A](x: A): Id[A] = x
  }

  "send notification by email" in new OfstedNotificationConf {
    val notifier = mock[Notifier[Id]]
    val client = new OfstedNotificationClient[Id](notifier)
    val builder = new FormLinkBuilder {}
    val formId = FormId("123")
    val notifyRequest = NotifyRequest(TemplateId("xxx"), EmailAddress("xx@yy"))

    (notifier
      .notifyByEmail(_: String, _: EmailAddress, _: Map[String, String])(_: MonadError[Id, String]))
      .expects(where {
        (_: String, emailAddress: EmailAddress, personalisation: Map[String, String], _: MonadError[Id, String]) =>
          emailAddress === notifyRequest.email
      })
      .returns(emailResponse)

    client.send(notifyRequest) must matchPattern {
      case OfstedNotificationClientResponse(_) =>
    }
  }

  private val emailResponse = new SendEmailResponse(
    """{
      | "id": "0d809b90-2431-4605-adcc-d32e77397989",
      | "content": {
      |   "notificationId":"0d809b90-2431-4605-adcc-d32e77397989", "reference":"null",
      |   "templateId":"99a38fa2-fe0b-4e14-b6-fd8d644f7a4d",
      |   "body": "'Hey Pas, I'm trying out Notify. Today is Thursday and my favourite colour is black.'",
      |   "subject": "some subject"
      | },
      | "template": {
      |   "id":"99a38fa2-fe0b-4e14-b6-fd8d644f7a4d",
      |   "version":"1",
      |   "uri":"'https://api.notifications.service.gov.uk/services'"
      | }
      |}""".stripMargin)
}
