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

import akka.actor.ActorSystem
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSProxyServer, WSRequest}
import uk.gov.hmrc.customs.api.common.config.ServicesConfig
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.http.ws.{WSPost, WSProxyConfiguration}

@Singleton
class WSProxyPost @Inject()(override val actorSystem: ActorSystem,
                            val config: Configuration,
                            ws: WSClient,
                            servicesConfig: ServicesConfig) extends HttpPost with WSPost {

  override val configuration = Some(config.underlying)
  override val hooks: Seq[HttpHook] = NoneRequired
  private def wsProxyServer: Option[WSProxyServer] = WSProxyConfiguration(s"${servicesConfig.env}.microservice.services.cygnum.proxy")

  override def buildRequest[A](url: String)(implicit hc: HeaderCarrier): WSRequest = {
    wsProxyServer match {
      case Some(proxy) => ws.url(url).withProxyServer(proxy)
      case None => ws.url(url)
    }
  }
}

