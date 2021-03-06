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

import java.time.Instant
import java.time.format.DateTimeFormatter

import cats.instances.int._
import cats.instances.string._
import cats.syntax.eq._

import scala.xml.{Elem, Node, NodeSeq, Text, TopScope}

object FormBundlePayloadPreprocessor {
  private val applicationFormsTag = "ApplicationForms"
  private val applicationFormTag = "ApplicationForm"
  private val parentIDTag = "ParentID"
  private val formIDTag = "FormID"
  private val createdDateTag = "CreatedDate"

  def apply(applicationForms: NodeSeq): Node = {
    val cleanedApplicationFormElements = (
      cleanApplicationFormElements _
        andThen removeDuplicateApplicationForms _
      andThen fillInAutomatedElementValues _)((applicationForms \\ applicationFormTag).collect { case e: Elem => e })
    correctNamespaces(<ApplicationForms>{cleanedApplicationFormElements}</ApplicationForms>)
  }

  private def correctNamespaces(doc: Elem): Elem =
    doc.copy(child = doc.child.map(stripNamespace))

  private def stripNamespace(node: Node): Node = node match {
    case e: Elem => e.copy(scope = TopScope, child = e.child.map(stripNamespace))
    case _ => node
  }


  private def fillInAutomatedElementValues(applicationForms: Seq[Elem]): Seq[Elem] = {
    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now)

    applicationForms.zipWithIndex.map { case (e, index) =>
      (replaceElementValue(parentIDTag, (if (index === 0) 0 else 1).toString) _
        andThen replaceElementValue(formIDTag, (index + 1).toString) _
        andThen replaceElementValue(createdDateTag, timestamp)) (e)
    }
  }

  private def removeDuplicateApplicationForms(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.distinct

  private def cleanApplicationFormElements(applicationForms: Seq[Elem]): Seq[Elem] =
    applicationForms.map(cleanApplicationForm)

  private def cleanApplicationForm(form: Elem): Elem =
    (removeChildElement(applicationFormsTag) _
      andThen clearElementValue(parentIDTag) _
      andThen clearElementValue(formIDTag) _
      andThen clearElementValue(createdDateTag) _)(form)

  private def replaceElementValue(label: String, value: String)(form: Elem) = {
    form.copy(child = form.child.collect {
      case c: Elem if (c.label === label) =>  c.copy(child = Seq(Text(value)))
      case n => n
    })
  }

  private def removeChildElement(label: String)(form: Elem) =
    form.copy(child = form.child.filterNot(_.label === label))

  private def clearElementValue(label: String)(form: Elem) = replaceElementValue(label, "")(form)
}
