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

package uk.gov.hmrc.ofstedformsproxy.config

import java.util.Base64

import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode: Mode = environment.mode
  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))
  lazy val cygnumURL : String = loadConfig("microservice.services.cygnum.url")
  lazy val getUrnXMLFileLocation : String = loadConfig("microservice.services.cygnum.getUrnXMLFileLocation")
  lazy val cygnumClientPassword : String = new String(Base64.getDecoder.decode(loadConfig("microservice.services.cygnum.client.base64KeystorePassword")))
  lazy val cygnumKeyStore : String = loadConfig("microservice.services.cygnum.client.base64Keystore")
  lazy val cygnumPrivateKeyAlias : String = new String(Base64.getDecoder.decode(loadConfig("microservice.services.cygnum.client.base64PrivateKeyAlias")))
  lazy val cygnumUsername : String = new String(Base64.getDecoder.decode(loadConfig("microservice.services.cygnum.base64Username")))
  lazy val cygnumPassword : String = new String(Base64.getDecoder.decode(loadConfig("microservice.services.cygnum.base64Password")))
}