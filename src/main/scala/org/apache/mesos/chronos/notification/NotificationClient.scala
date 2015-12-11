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

import java.util.logging.{ Level, Logger }

import akka.actor.{ Actor, Terminated }
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

/**
 * The form and design of the ability to send a notification to a specific source
 * @author Greg Bowyer (gbowyer@fastmail.co.uk)
 */
trait NotificationClient extends Actor {

  private[this] val log = Logger.getLogger(getClass.getName)

  /**
   * Send the notification
   * @param job the job that is being notified on
   * @param to the recipient of the notification
   * @param subject the subject line to use in notification
   * @param message the message that offers additional information about the notification
   */
  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String])

  def receive = {
    case (job: BaseJob, to: String, subject: String, message: Option[String] @unchecked) =>
      try {
        sendNotification(job, to, subject, message)
      }
      catch {
        case t: Exception => log.log(Level.WARNING, "Caught a Exception while trying to send mail.", t)
      }
    case Terminated(_) =>
      log.warning("Actor has exited, no longer sending out email notifications!")
    case _ => log.warning("Couldn't understand message.")
  }

}
