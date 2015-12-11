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

import java.io.{ DataOutputStream, StringWriter }
import java.net.{ HttpURLConnection, URL }
import java.util.logging.Logger

import com.fasterxml.jackson.core.JsonFactory
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

class SlackClient(val webhookUrl: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {

    val jsonBuffer = new StringWriter
    val factory = new JsonFactory()
    val generator = factory.createGenerator(jsonBuffer)

    // Create the payload
    generator.writeStartObject()

    if (message.nonEmpty && message.get.nonEmpty) {
      if (subject != null && subject.nonEmpty) {
        generator.writeStringField("text", "%s: %s".format(subject, message.get))
      }
      else {
        generator.writeStringField("text", "%s".format(message.get))
      }
    }

    generator.writeEndObject()
    generator.flush()

    val payload = jsonBuffer.toString

    var connection: HttpURLConnection = null
    try {
      val url = new URL(webhookUrl)
      connection = url.openConnection.asInstanceOf[HttpURLConnection]
      connection.setDoInput(true)
      connection.setDoOutput(true)
      connection.setUseCaches(false)
      connection.setRequestMethod("POST")

      val outputStream = new DataOutputStream(connection.getOutputStream)
      outputStream.writeBytes(payload)
      outputStream.flush()
      outputStream.close()

      log.info("Sent message to Slack! Response code:" +
        connection.getResponseCode +
        " - " +
        connection.getResponseMessage)
    }
    finally {
      if (connection != null) {
        connection.disconnect()
      }
    }
  }
}
