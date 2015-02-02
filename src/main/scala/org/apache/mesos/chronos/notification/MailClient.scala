package org.apache.mesos.chronos.notification

import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import org.apache.commons.mail.{DefaultAuthenticator, SimpleEmail}

/**
 * A very simple mail client that works out of the box with providers such as Amazon SES.
 * TODO(FL): Test with other providers.

 * @author Florian Leibert (flo@leibert.de)
 */
class MailClient(
                  val mailServerString: String,
                  val fromUser: String,
                  val mailUser: Option[String],
                  val password: Option[String],
                  val ssl: Boolean)
  extends NotificationClient {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val split = """(.*):([0-9]*)""".r
  private[this] val split(mailHost, mailPortStr) = mailServerString
  val mailPort = mailPortStr.toInt

  def sendNotification(job: BaseJob, to: String, subject: String, message: Option[String]) {
    val email = new SimpleEmail
    email.setHostName(mailHost)

    if (mailUser.isDefined && password.nonEmpty) {
      email.setAuthenticator(new DefaultAuthenticator(mailUser.get, password.get))
    }

    email.addTo(to)
    email.setFrom(fromUser)

    email.setSubject(subject)

    if (message.nonEmpty && message.get.nonEmpty) {
      email.setMsg(message.get)
    }

    email.setSSLOnConnect(ssl)
    email.setSmtpPort(mailPort)
    val response = email.send
    log.info("Sent email to '%s' with subject: '%s', got response '%s'".format(to, subject, response))
  }

}
