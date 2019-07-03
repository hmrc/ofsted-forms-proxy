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

import cats.{Id, MonadError}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.ofstedformsproxy.notification.{EmailAddress, Notifier, OfstedNotificationClient, TemplateId}

class NotifierRequestHandlerSpec extends WordSpec with MustMatchers with MockFactory {

  implicit val me = new MonadError[Id, String] {
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    override def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = ???
    override def raiseError[A](e: String): Id[A] = e.asInstanceOf[A]
    override def handleErrorWith[A](fa: Id[A])(f: String => Id[A]): Id[A] = ???
    override def pure[A](x: A): Id[A] = x
  }


  "return 200 if email is sent" in {
    val notifier = mock[Notifier[Id]]
    val client = new OfstedNotificationClient[Id](notifier)
    val handler = new NotifierRequestHandler[Id](client)
    val notifyRequest = NotifyRequest(TemplateId("123"), EmailAddress("some@else"), Map("firstName" -> "Tom", "lastName" -> "Cruise"))

    (notifier.notifyByEmail(_: String, _: EmailAddress, _: Map[String, String])(_: MonadError[Id, String]))
      .expects(where {
        (id: String, email: EmailAddress, _: Map[String, String], _: MonadError[Id, String]) =>
          id == "123" && email === EmailAddress("some@else")
      })

    handler.handleRequest(notifyRequest) mustBe Response(200, "")
  }
}
