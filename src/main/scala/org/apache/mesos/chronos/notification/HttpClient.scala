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
import org.apache.commons.codec.binary.Base64
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

class HttpClient(val endpointUrl: String,
                 val credentials: Option[String]) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {

    val jsonBuffer = new StringWriter
    val factory = new JsonFactory()
    val generator = factory.createGenerator(jsonBuffer)

    // Create the payload
    generator.writeStartObject()

    if (subject != null && subject.nonEmpty) {
      generator.writeStringField("subject", subject)
    }
    if (message.nonEmpty && message.get.nonEmpty) {
      generator.writeStringField("message", message.get)
    }
    if (to != null && to.nonEmpty) {
      generator.writeStringField("to", to)
    }
    generator.writeStringField("job", job.name.toString())
    generator.writeStringField("command", job.command.toString())
    generator.writeStringField("cpus", job.cpus.toString())
    generator.writeStringField("async", job.async.toString())
    generator.writeStringField("softError", job.softError.toString())
    generator.writeStringField("epsilon", job.epsilon.toString())
    generator.writeStringField("errorCount", job.errorCount.toString())
    generator.writeStringField("errorsSinceLastSuccess", job.errorsSinceLastSuccess.toString())
    generator.writeStringField("executor", job.executor.toString())
    generator.writeStringField("executorFlags", job.executorFlags.toString())
    generator.writeStringField("lastError", job.lastError.toString())
    generator.writeStringField("lastSuccess", job.lastSuccess.toString())
    generator.writeStringField("mem", job.mem.toString())
    generator.writeStringField("retries", job.retries.toString())
    generator.writeStringField("successCount", job.successCount.toString())
    generator.writeStringField("uris", job.uris.mkString(","))

    generator.writeEndObject()
    generator.flush()

    val payload = jsonBuffer.toString
    val auth = if (credentials.nonEmpty && credentials.get.nonEmpty) {
      "Basic " + new String(Base64.encodeBase64(credentials.get.getBytes()));
    }
    else {
      ""
    }

    var connection: HttpURLConnection = null
    try {
      val url = new URL(endpointUrl)
      connection = url.openConnection.asInstanceOf[HttpURLConnection]
      connection.setDoInput(true)
      connection.setDoOutput(true)
      connection.setUseCaches(false)
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json");

      if (auth.nonEmpty) {
        connection.setRequestProperty("Authorization", auth);
      }

      val outputStream = new DataOutputStream(connection.getOutputStream)
      outputStream.writeBytes(payload)
      outputStream.flush()
      outputStream.close()

      log.info("Sent message to http endpoint. Response code:" +
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
