package com.airbnb.notification

import java.util.logging.{Level, Logger}
import com.airbnb.scheduler.jobs.BaseJob
import akka.actor.{Actor, Terminated}

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
  def sendNotification(job: BaseJob, to : String, subject : String, message : Option[String])

  def receive = {
    case (job: BaseJob, to: String, subject: String, message: Option[String]) =>
      try {
        sendNotification(job, to, subject, message)
      } catch {
        case t: Exception => log.log(Level.WARNING, "Caught a Exception while trying to send mail.", t)
      }
    case Terminated(_) =>
      log.warning("Actor has exited, no longer sending out email notifications!")
    case _ => log.warning("Couldn't understand message.")
  }

}
