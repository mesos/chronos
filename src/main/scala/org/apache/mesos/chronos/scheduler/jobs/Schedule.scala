package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{DateTime, Period}


case class Schedule(@JsonProperty schedule: String,
                    @JsonProperty scheduleTimeZone: String,
                    @JsonProperty invocationTime: DateTime,
                    @JsonProperty originTime: DateTime,
                    @JsonProperty offset: Long = 0,
                    @JsonProperty recurrences: Option[Long] = None,
                    @JsonProperty period: Period) {


  def next: Option[Schedule] = {
    if (recurrences.exists(_ == 0)) None
    else {
      val nextOffset = offset + 1
      val nextInvocationTime = Schedule.addPeriods(originTime, period, nextOffset.asInstanceOf[Int] /* jodatime doesn't support longs in their plus methods. What to do about overflow? */)

      Some(Schedule(schedule, scheduleTimeZone, nextInvocationTime, originTime, nextOffset, recurrences.map(_ - 1), period))
    }
  }

  def toZeroOffsetISO8601Representation: String = {
    Iso8601Expressions.create(recurrences.getOrElse(-1), invocationTime, period)
  }
}

object Schedule {
  def parse(scheduleStr: String, timeZoneStr: String = ""): Option[Schedule] = {
    Iso8601Expressions.parse(scheduleStr, timeZoneStr).map {
      case (recurrences, start, period) =>
        Schedule(scheduleStr, timeZoneStr, start, start, offset = 0, if (recurrences == -1) None else Some(recurrences), period)
    }
  }


  // replace with multiplied by?
  def addPeriods(origin: DateTime, period: Period, number: Int): DateTime = {
    origin.plus(period.multipliedBy(number))
  }
}
