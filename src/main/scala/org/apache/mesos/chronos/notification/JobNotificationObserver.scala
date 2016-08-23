package org.apache.mesos.chronos.notification

import java.util.logging.Logger

import akka.actor.ActorRef
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.jobs._
import org.joda.time.{DateTimeZone, DateTime}

class JobNotificationObserver @Inject()(val notificationClients: List[ActorRef] = List(),
                                      val clusterName: Option[String] = None) {
  private[this] val log = Logger.getLogger(getClass.getName)
  val clusterPrefix = clusterName.map(name => s"[$name]").getOrElse("")

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

    case JobFinished(job, taskStatus, attempts) =>
      val msg = "\n'%s'. Retries attempted: %d.\nTask id: %s\n"
        .format(DateTime.now(DateTimeZone.UTC), job.retries, taskStatus.getTaskId.getValue)
      sendNotification(job, "%s [Chronos] job '%s' finished!".format(clusterPrefix, job.name),
        Some(TaskUtils.appendSchedulerMessage(msg, taskStatus)))
  }, getClass.getSimpleName)

  def sendNotification(job: BaseJob, subject: String, message: Option[String] = None) {
    for (client <- notificationClients) {
      val subowners = job.owner.split("\\s*,\\s*")
      for (subowner <- subowners) {
        log.info("Sending mail notification to:%s for job %s using client: %s".format(subowner, job.name, client))
        client !(job, subowner, subject, message)
      }
    }

    log.info(subject)
  }

}
