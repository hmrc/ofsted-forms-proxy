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

package uk.gov.hmrc.ofstedformsproxy

import cats.MonadError

import scala.util.{Failure, Try}

package object handlers {

  implicit val me = new MonadError[Try, String] {
    override def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => Try[Either[A, B]]): Try[B] = ???
    override def raiseError[A](e: String): Try[A] = Failure(new Exception(e))
    override def handleErrorWith[A](fa: Try[A])(f: String => Try[A]): Try[A] = fa.recoverWith {
      case e: Throwable => Failure(e)
    }

    override def pure[A](x: A): Try[A] = Try(x)
  }
}
