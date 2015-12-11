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

import net.kencochrane.raven.RavenFactory
import net.kencochrane.raven.event.{ Event, EventBuilder }
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

/**
 * Notification client that uses sentry / raven to transmit its messages
 * @author Greg Bowyer (gbowyer@fastmail.co.uk)
 */
class RavenClient(val dsn: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val raven = RavenFactory.ravenInstance(dsn)

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {
    val ravenMessage = subject + "\n\n" + message.getOrElse("")
    val event = new EventBuilder()
      .setMessage(ravenMessage)
      .setLevel(Event.Level.ERROR)
      .addTag("owner", to)
      .addTag("job", job.name)
      .addTag("command", job.command)
      .addExtra("cpus", job.cpus)
      .addExtra("async", job.async)
      .addExtra("softError", job.softError)
      .addExtra("epsilon", job.epsilon)
      .addExtra("errorCount", job.errorCount)
      .addExtra("errorsSinceLastSuccess", job.errorsSinceLastSuccess)
      .addExtra("executor", job.executor)
      .addExtra("executorFlags", job.executorFlags)
      .addExtra("lastError", job.lastError)
      .addExtra("lastSuccess", job.lastSuccess)
      .addExtra("mem", job.mem)
      .addExtra("retries", job.retries)
      .addExtra("successCount", job.successCount)
      .addExtra("uris", job.uris.mkString(","))
      .build()

    raven.sendEvent(event)
  }

}
