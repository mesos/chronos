package org.apache.mesos.chronos.notification

import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.event.{Event, EventBuilder}

/**
 * Notification client that uses sentry / raven to transmit its messages
 * @author Greg Bowyer (gbowyer@fastmail.co.uk)
 */
class RavenClient(val dsn: String) extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val raven = RavenFactory.ravenInstance(dsn)

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {
    val ravenMessage = subject + "\n\n" + message.getOrElse("")
    val uris = job.fetch.map { _.uri } ++ job.uris
    val event = new EventBuilder()
      .withMessage(ravenMessage)
      .withLevel(Event.Level.ERROR)
      .withTag("owner", to)
      .withTag("job", job.name)
      .withTag("command", job.command)
      .withExtra("cpus", job.cpus)
      .withExtra("async", job.async)
      .withExtra("softError", job.softError)
      .withExtra("epsilon", job.epsilon)
      .withExtra("errorCount", job.errorCount)
      .withExtra("errorsSinceLastSuccess", job.errorsSinceLastSuccess)
      .withExtra("executor", job.executor)
      .withExtra("executorFlags", job.executorFlags)
      .withExtra("lastError", job.lastError)
      .withExtra("lastSuccess", job.lastSuccess)
      .withExtra("mem", job.mem)
      .withExtra("retries", job.retries)
      .withExtra("successCount", job.successCount)
      .withExtra("uris", uris.mkString(","))
      .build()

    raven.sendEvent(event)
  }

}
