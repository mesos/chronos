package com.airbnb.notification

import java.util.logging.{Level, Logger}
import akka.actor.{Terminated, Actor}
import net.kencochrane.raven.{Raven,RavenFactory}
import net.kencochrane.raven.event.{Event, EventBuilder}
import com.airbnb.scheduler.jobs.BaseJob

/**
 * Notification client that uses sentry / raven to transmit its messages
 * @author Greg Bowyer (gbowyer@fastmail.co.uk)
 */
class RavenClient(val dsn: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val raven = RavenFactory.ravenInstance(dsn)

  def sendNotification(job: BaseJob, to : String, subject : String, message : Option[String]) {
    val ravenMessage = subject + "\n\n" + message.getOrElse("")
    val event = new EventBuilder()
      .setMessage(ravenMessage)
      .setLevel(Event.Level.ERROR)
      .addTag("owner", to)
      .addTag("job", job.name)
      .addTag("command", job.command)
      .addExtra("cpus", job.cpus)
      .addExtra("async", job.async)
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
