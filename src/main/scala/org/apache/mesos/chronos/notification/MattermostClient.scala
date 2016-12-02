package org.apache.mesos.chronos.notification

import java.io.{DataOutputStream, StringWriter}
import java.net.{HttpURLConnection, URL}
import java.util.logging.Logger

import com.fasterxml.jackson.core.JsonFactory
import org.apache.mesos.chronos.scheduler.jobs.BaseJob

class MattermostClient(val webhookUrl: String) extends NotificationClient {

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
      } else {
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
      connection.setRequestProperty("Content-Type", "application/json");

      val outputStream = new DataOutputStream(connection.getOutputStream)
      outputStream.writeBytes(payload)
      outputStream.flush()
      outputStream.close()

      log.info("Sent message to Mattermost! Response code:" +
        connection.getResponseCode +
        " - " +
        connection.getResponseMessage)
    } finally {
      if (connection != null) {
        connection.disconnect()
      }
    }
  }
}
