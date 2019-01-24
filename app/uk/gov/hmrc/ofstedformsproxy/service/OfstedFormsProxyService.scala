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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import scalaz.{-\/, \/, \/-}
import uk.gov.hmrc.ofstedformsproxy.models.{GetUrnFailure, GetUrnSuccess}

//TODO: REMOVE THIS TRANSLATION UNIT
//@ImplementedBy(classOf[OfstedProxyServiceImpl])
//trait OfstedProxyService {
//
//  def getUrn(): GetUrnFailure \/ GetUrnSuccess
//}

//TODO: process the request body
//TODO: call the post method via the connector interface
//TODO: the payload body needs to be a string representation
//TODO: construct the soap envelope
//TODO: construct the message
//TODO: process response
//TODO: extract the urn

//@Singleton
//class OfstedProxyServiceImpl @Inject()(ss: SOAPMessageService) extends OfstedProxyService {
//
//  //  override def getUrn(): GetUrnFailure \/ GetUrnSuccess = {
//  //    ss.readInXMLPayload("/conf/xml/GetNewURN.xml") match {
//  //      case \/-(document) => \/-(GetUrnSuccess("Got document"))
//  //      case -\/(error) => -\/(GetUrnFailure("failed"))
//  //    }
//  //  }
//
//  override def getUrn(): GetUrnFailure \/ GetUrnSuccess = {
//    val result: String \/ GetUrnSuccess = for {
//      document <- ss.readInXMLPayload("/conf/xml/GetNewURN.xml")
//      //      soapMessage <- ss.createSOAPEnvelope(document)
//      //      msg         <- ss.signSOAPMessage(soapMessage)
//    } yield GetUrnSuccess("1234")
//
//    result match {
//      case \/-(s)  => s.message
//      case -\/(e) => GetUrnFailure(e)
//    }
//  }
//
//
//
//}
