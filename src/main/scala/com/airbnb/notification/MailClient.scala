package com.airbnb.notification

import java.util.logging.{Level, Logger}
import akka.actor.{Terminated, Actor}
import org.apache.commons.mail.{DefaultAuthenticator, SimpleEmail}
import com.airbnb.scheduler.jobs.BaseJob

/**
 * A very simple mail client that works out of the box with providers such as Amazon SES.
 * TODO(FL): Test with other providers.

 * @author Florian Leibert (flo@leibert.de)
 */
class MailClient(
    val mailServerString : String,
    val fromUser : String,
    val mailUser : Option[String],
    val password : Option[String],
    val ssl : Boolean)
  extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val split = """(.*):([0-9]*)""".r
  private[this] val split(mailHost, mailPortStr) = mailServerString

  val mailPort = mailPortStr.toInt
  def sendNotification(job: BaseJob, to : String, subject : String, message : Option[String]) {
    val email = new SimpleEmail
    email.setHostName(mailHost)

    if (!mailUser.isEmpty && !password.isEmpty) {
      email.setAuthenticator(new DefaultAuthenticator(mailUser.get, password.get))
    }

    email.addTo(to)
    email.setFrom(fromUser)

    email.setSubject(subject)

    if (!message.isEmpty) {
      email.setMsg(message.get)
    }

    email.setSSLOnConnect(ssl)
    email.setSmtpPort(mailPort)
    val response = email.send
    log.info("Sent email to '%s' with subject: '%s', got response '%s'".format(to, subject, response))
  }

}
