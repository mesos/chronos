package org.apache.mesos.chronos.utils

import java.util.concurrent.TimeUnit

import org.joda.time.{ DateTime, DateTimeZone }

import scala.concurrent.duration.FiniteDuration
import scala.math.Ordered

/**
  * An ordered wrapper for UTC timestamps.
  */
abstract case class Timestamp private (utcDateTime: DateTime) extends Ordered[Timestamp] {
  def compare(that: Timestamp): Int = this.utcDateTime compareTo that.utcDateTime

  override def toString: String = utcDateTime.toString

  def toDateTime: DateTime = utcDateTime

  def until(other: Timestamp): FiniteDuration = {
    val millis = other.utcDateTime.getMillis - utcDateTime.getMillis
    FiniteDuration(millis, TimeUnit.MILLISECONDS)
  }

  def +(duration: FiniteDuration): Timestamp = Timestamp(utcDateTime.getMillis + duration.toMillis)
  def -(duration: FiniteDuration): Timestamp = Timestamp(utcDateTime.getMillis - duration.toMillis)
}

object Timestamp {
  /**
    * Returns a new Timestamp representing the instant that is the supplied
    * dateTime converted to UTC.
    */
  def apply(dateTime: DateTime): Timestamp = new Timestamp(dateTime.toDateTime(DateTimeZone.UTC)) {}

  /**
    * Returns a new Timestamp representing the instant that is the supplied
    * number of milliseconds after the epoch.
    */
  def apply(ms: Long): Timestamp = Timestamp(new DateTime(ms))

  /**
    * Returns a new Timestamp representing the supplied time.
    *
    * See the Joda time documentation for a description of acceptable formats:
    * http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()
    */
  def apply(time: String): Timestamp = Timestamp(DateTime.parse(time))

  /**
    * Returns a new Timestamp representing the current instant.
    */
  def now(): Timestamp = Timestamp(System.currentTimeMillis)

}
