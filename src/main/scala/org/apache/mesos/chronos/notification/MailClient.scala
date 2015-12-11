/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.notification

import java.util.logging.Logger

import org.apache.commons.mail.{ DefaultAuthenticator, SimpleEmail }
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

/**
 * A very simple mail client that works out of the box with providers such as Amazon SES.
 * TODO(FL): Test with other providers.
 *
 * @author Florian Leibert (flo@leibert.de)
 */
class MailClient(
  val mailServerString: String,
  val fromUser: String,
  val mailUser: Option[String],
  val password: Option[String],
  val ssl: Boolean)
    extends NotificationClient {

  val mailPort = mailPortStr.toInt
  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val split = """(.*):([0-9]*)""".r
  private[this] val split(mailHost, mailPortStr) = mailServerString

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {
    val email = new SimpleEmail
    email.setHostName(mailHost)

    if (mailUser.isDefined && password.nonEmpty) {
      email.setAuthenticator(new DefaultAuthenticator(mailUser.get, password.get))
    }

    email.addTo(to)
    email.setFrom(fromUser)

    email.setSubject(subject)

    if (message.nonEmpty && message.get.nonEmpty) {
      email.setMsg(message.get)
    }

    email.setSSLOnConnect(ssl)
    email.setSmtpPort(mailPort)
    val response = email.send
    log.info("Sent email to '%s' with subject: '%s', got response '%s'".format(to, subject, response))
  }

}
