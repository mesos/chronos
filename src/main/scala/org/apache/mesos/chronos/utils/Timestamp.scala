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

  /**
   * Returns a new Timestamp representing the instant that is the supplied
   * number of milliseconds after the epoch.
   */
  def apply(ms: Long): Timestamp = Timestamp(new DateTime(ms))

  /**
   * Returns a new Timestamp representing the instant that is the supplied
   * dateTime converted to UTC.
   */
  def apply(dateTime: DateTime): Timestamp = new Timestamp(dateTime.toDateTime(DateTimeZone.UTC)) {}

}
