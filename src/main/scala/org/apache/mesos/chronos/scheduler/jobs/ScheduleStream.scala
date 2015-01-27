package org.apache.mesos.chronos.scheduler.jobs

/**
 * A stream of schedules.
 * Calling tail will return a clipped schedule.
 * The schedule consists of a string representation of an ISO8601 expression as well as a BaseJob.
 * @author Florian Leibert (flo@leibert.de)
 */
class ScheduleStream(val schedule: String, val jobName: String, val scheduleTimeZone: String = "") {

  def head(): (String, String, String) = {
    (schedule, jobName, scheduleTimeZone)
  }

  /**
   * Returns a clipped schedule.
   * @return
   */
  def tail(): Option[ScheduleStream] = {
    //TODO(FL) Represent the schedule as a data structure instead of a string.
    Iso8601Expressions.parse(schedule, scheduleTimeZone) match {
      case Some((rec, start, per)) =>
        if (rec == -1)
          return Some(new ScheduleStream(Iso8601Expressions.create(rec, start.plus(per), per), jobName,
            scheduleTimeZone))
        else if (rec > 0)
          return Some(new ScheduleStream(Iso8601Expressions.create(rec - 1, start.plus(per), per), jobName,
            scheduleTimeZone))
        else
          return None
      case None =>
        None
    }
  }
}
