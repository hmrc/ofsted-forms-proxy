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

import org.scalatest.{MustMatchers, WordSpec}

import scala.xml.{Elem, PrettyPrinter}

class FormBundlePayloadPreprocessorSpec extends WordSpec with MustMatchers {
  private val printer = new PrettyPrinter(100, 2)

  "Rewrites single ApplicationForms to have the correct FormID and ParentFormID" in {
    validate(
      <ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo/><Bar>baz</Bar></ApplicationForm></ApplicationForms>,
      <ApplicationForms><ApplicationForm><FormID>1</FormID><ParentID>1</ParentID><Foo/><Bar>baz</Bar></ApplicationForm></ApplicationForms>
    )
  }

  "Unnests ApplicationForms and inserts the correct FormID and ParentFormID" in {
    validate(
      <ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>1</Foo></ApplicationForm><ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>2</Foo></ApplicationForm><ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>3</Foo></ApplicationForm></ApplicationForms></ApplicationForms></ApplicationForms>,
      <ApplicationForms><ApplicationForm><FormID>1</FormID><ParentID>1</ParentID><Foo>1</Foo></ApplicationForm><ApplicationForm><FormID>2</FormID><ParentID>0</ParentID><Foo>2</Foo></ApplicationForm><ApplicationForm><FormID>3</FormID><ParentID>0</ParentID><Foo>3</Foo></ApplicationForm></ApplicationForms>
    )
  }

  "Removes duplicates ApplicationForm elements" in {
    validate(
      <ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>1</Foo></ApplicationForm><ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>1</Foo></ApplicationForm><ApplicationForms><ApplicationForm><FormID>toBeReplaced</FormID><ParentID>thisToo</ParentID><Foo>2</Foo></ApplicationForm></ApplicationForms></ApplicationForms></ApplicationForms>,
      <ApplicationForms><ApplicationForm><FormID>1</FormID><ParentID>1</ParentID><Foo>1</Foo></ApplicationForm><ApplicationForm><FormID>2</FormID><ParentID>0</ParentID><Foo>2</Foo></ApplicationForm></ApplicationForms>
    )
  }

  private def validate(in: Elem, expected: Elem) =
    printer.format(FormBundlePayloadPreprocessor(in)) mustBe printer.format(expected)
}
