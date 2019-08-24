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
import cats.instances.string._
import cats.syntax.eq._

import scala.xml.{Elem, Node, NodeSeq, Text, TopScope}

object FormBundlePayloadPreprocessor {
  def apply(applicationForms: NodeSeq): Node = {
    val cleanedApplicationFormElements = (
      cleanIndividualApplicationForms _
        andThen removeDuplicateApplicationForms _
      andThen addIdElements _)((applicationForms \\ "ApplicationForm").collect { case e: Elem => e })
    correctNamespaces(<ApplicationForms>{cleanedApplicationFormElements}</ApplicationForms>)
  }

  private def correctNamespaces(doc: Elem): Elem =
    doc.copy(child = doc.child.map(stripNamespace))

  private def stripNamespace(node: Node): Node = node match {
    case e: Elem => e.copy(scope = TopScope, child = e.child.map(stripNamespace))
    case _ => node
  }


  private def addIdElements(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.zipWithIndex.map { case (e, index) =>
      (replaceElementValue("ParentID", (if (index === 0) 0 else 1).toString) _
        andThen replaceElementValue("FormID", (index + 1).toString) _)(e)
    }

  private def removeDuplicateApplicationForms(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.distinct

  private def cleanIndividualApplicationForms(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms map { (removeChildElement("ApplicationForms")) }

  private def replaceElementValue(label: String, value: String)(form: Elem) = {
    form.copy(child = form.child.collect {
      case c: Elem if (c.label === label) =>  c.copy(child = Seq(Text(value)))
      case n => n
    })
  }

  private def removeChildElement(label: String)(form: Elem) =
    form.copy(child = form.child.filterNot(_.label === label))
}
