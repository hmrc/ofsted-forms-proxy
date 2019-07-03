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

import cats.{Applicative, Id, MonadError}
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.ofstedformsproxy.notification.{EmailAddress, Notifier, OfstedNotificationClient, TemplateId}
import uk.gov.service.notify.SendEmailResponse

import scala.util.{Failure, Success, Try}

class NotifierRequestHandlerSpec extends WordSpec with MustMatchers {

  implicit val me = new MonadError[Id, String] {
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    override def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = ???
    override def raiseError[A](e: String): Id[A] = e.asInstanceOf[A]
    override def handleErrorWith[A](fa: Id[A])(f: String => Id[A]): Id[A] = ???
    override def pure[A](x: A): Id[A] = x
  }

  "return 200 if email is sent" in {
    val notifier = new Notifier[Id] {
      override def notifyByEmail(notifyRequest: NotifyRequest)(
        implicit me: MonadError[Id, String], M: Applicative[Id]): Id[SendEmailResponse] = emailResponse
    }

    val client = new OfstedNotificationClient[Id](notifier)
    val handler = new NotifierRequestHandler[Id](client)
    val notifyRequest = NotifyRequest(TemplateId("123"), EmailAddress("some@else"), Map("firstName" -> "Tom", "lastName" -> "Cruise"))

    handler.handleRequest(notifyRequest) mustBe Response(200, "")
  }

  "return 500 with error msg if email sent fail" in {
    val errorMsg = "unable to send email"
    val notifyRequest = NotifyRequest(TemplateId("123"), EmailAddress("some@else"), Map("firstName" -> "Tom", "lastName" -> "Cruise"))
    val notifier = new Notifier[Try] {
        override def notifyByEmail(notifyRequest: NotifyRequest)(
          implicit me: MonadError[Try, String], M: Applicative[Try]): Try[SendEmailResponse] = Try(throw new Exception(errorMsg))
      }

    val client = new OfstedNotificationClient[Try](notifier)
    val handler = new NotifierRequestHandler[Try](client)

    val actualResponse = handler.handleRequest(notifyRequest)(uk.gov.hmrc.ofstedformsproxy.handlers.me)

    actualResponse.isFailure mustBe true
    actualResponse.failed.get.getMessage mustBe errorMsg
  }


  "send email" ignore {
    val client = new OfstedNotificationClient[Id](new Notifier[Id] {})
    val handler = new NotifierRequestHandler[Id](client)
    val notifyRequest = NotifyRequest(TemplateId("339fc6bd-8369-4a33-9d1a-e0607c63a1e2"), EmailAddress("pasquale.gatto@digital.hmrc.gov.uk"),
      Map("formId" -> "222", "firstName" -> "Tommy", "lastName" -> "Cruise"))

    handler.handleRequest(notifyRequest) mustBe Response(200, "")
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
