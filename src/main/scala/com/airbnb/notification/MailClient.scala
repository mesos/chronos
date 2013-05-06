package com.airbnb.notification

import java.text.SimpleDateFormat
import java.util.logging.{Level, Logger}
import scala.actors.{Exit, Actor}

import org.apache.commons.mail.{DefaultAuthenticator, SimpleEmail}

/**
 * A very simple mail client that works out of the box with providers such as Amazon SES.
 * TODO(FL): Test with other providers and adjust configuration to allow disabling SSL, etc.

 * @author Florian Leibert (flo@leibert.de)
 */
class MailClient(
    val mailServerString : String,
    val fromUser : String,
    val mailUser : Option[String],
    val password : Option[String])
  extends Actor {

  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val split = """(.*):([0-9]*)""".r
  private[this] val split(mailHost, mailPortStr) = mailServerString

  val mailPort = mailPortStr.toInt
  def sendNotification(to : String, subject : String, message : Option[String]) {
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

    email.setSSLOnConnect(true)
    email.setSmtpPort(mailPort)
    val response = email.send
    log.info("Sent email to '%s' with subject: '%s', got response '%s'".format(to, subject, response))
  }

  def act() {
    loop {
      react {
        case (to : String, subject : String, message : Option[String]) => {
          try {
            sendNotification(to, subject, message)
          } catch {
            case t: Throwable => log.log(Level.WARNING, "Caught a throwable while trying to send mail.", t)
          }
        }
        case Exit (worker: Actor, reason: AnyRef) => {
          log.warning("Actor has exited, no longer sending out email notifications!")
        }
        case _ => log.warning("Couldn't understand message.")
      }
    }
  }
}