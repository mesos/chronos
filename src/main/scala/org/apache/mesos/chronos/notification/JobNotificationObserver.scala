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

import akka.actor.ActorRef
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.jobs._
import org.joda.time.{ DateTime, DateTimeZone }

class JobNotificationObserver @Inject() (val notificationClients: List[ActorRef] = List(),
                                         val clusterName: Option[String] = None) {
  val clusterPrefix = clusterName.map(name => s"[$name]").getOrElse("")
  private[this] val log = Logger.getLogger(getClass.getName)

  def asObserver: JobsObserver.Observer = JobsObserver.withName({
    case JobRemoved(job) => sendNotification(job, "%s [Chronos] Your job '%s' was deleted!".format(clusterPrefix, job.name), None)
    case JobDisabled(job, cause) => sendNotification(
      job,
      "%s [Chronos] job '%s' disabled".format(clusterPrefix, job.name),
      Some(cause))

    case JobRetriesExhausted(job, taskStatus, attempts) =>
      val msg = "\n'%s'. Retries attempted: %d.\nTask id: %s\n"
        .format(DateTime.now(DateTimeZone.UTC), job.retries, taskStatus.getTaskId.getValue)
      sendNotification(job, "%s [Chronos] job '%s' failed!".format(clusterPrefix, job.name),
        Some(TaskUtils.appendSchedulerMessage(msg, taskStatus)))
  }, getClass.getSimpleName)

  def sendNotification(job: BaseJob, subject: String, message: Option[String] = None) {
    for (client <- notificationClients) {
      val subowners = job.owner.split("\\s*,\\s*")
      for (subowner <- subowners) {
        log.info("Sending mail notification to:%s for job %s using client: %s".format(subowner, job.name, client))
        client ! (job, subowner, subject, message)
      }
    }

    log.info(subject)
  }

}
