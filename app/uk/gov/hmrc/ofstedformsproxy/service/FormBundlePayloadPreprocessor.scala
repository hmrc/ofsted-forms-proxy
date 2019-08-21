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

package uk.gov.hmrc.ofstedformsproxy.service

import cats.instances.int._
import cats.syntax.eq._

import scala.xml.{Elem, Node, NodeSeq}

object FormBundlePayloadPreprocessor {
  def apply(applicationForms: NodeSeq): Node = {
    val cleanedApplicationFormElements = (
      cleanIndividualApplicationForms _
        andThen removeDuplicateApplicationForms _
      andThen addIdElements _)((applicationForms \\ "ApplicationForm").collect { case e: Elem => e })
    <ApplicationForms>{cleanedApplicationFormElements}</ApplicationForms>
  }

  private def addIdElements(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.zipWithIndex.map { case (e, index) =>
      (addChildElement(<ParentID>{if (index === 0) 1 else 0}</ParentID>) _
      andThen addChildElement(<FormID>{index + 1}</FormID>) _)(e)
    }

  private def removeDuplicateApplicationForms(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.distinct

  private def cleanIndividualApplicationForms(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms map {
      (removeChildElement("ApplicationForms") _
        andThen removeChildElement("ParentID") _
        andThen removeChildElement("FormID") _)
    }

  private def addChildElement(elem: Elem)(form: Elem) =
    form.copy(child = elem :: form.child.toList)

  private def removeChildElement(label: String)(form: Elem) =
    form.copy(child = form.child.filterNot(_.label == label))
}
