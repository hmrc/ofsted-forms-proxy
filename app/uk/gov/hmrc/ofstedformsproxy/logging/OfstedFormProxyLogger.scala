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

package uk.gov.hmrc.ofstedformsproxy.logging

import com.google.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.customs.api.common.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ofstedformsproxy.logging.LoggingHelper._
import uk.gov.hmrc.ofstedformsproxy.models.SeqOfHeader

@Singleton
class OfstedFormProxyLogger @Inject()(serviceConfig: ServicesConfig) {

  private lazy val loggerName: String = serviceConfig.getString("application.logger.name")
  lazy val logger = play.api.Logger(loggerName)

  def debug(msg: => String, url: => String, payload: => String)(implicit hc: HeaderCarrier): Unit = logger.debug(formatDebug(msg, Some(url), Some(payload)))

  def debug(msg: => String, headers: => SeqOfHeader): Unit = logger.debug(formatDebug(msg, headers))

  def info(msg: => String)(implicit hc: HeaderCarrier): Unit = logger.info(formatInfo(msg))

  def error(msg: => String)(implicit hc: HeaderCarrier): Unit = logger.error(formatError(msg))

  def error(msg: => String, e: => Throwable)(implicit hc: HeaderCarrier): Unit = logger.error(formatError(msg), e)
}
