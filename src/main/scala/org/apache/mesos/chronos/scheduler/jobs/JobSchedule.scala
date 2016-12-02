package org.apache.mesos.chronos.scheduler.jobs

class JobSchedule(val schedule: String, val jobName: String, val scheduleTimeZone: String = "") {
  def getSchedule: Option[JobSchedule] =
  //TODO(FL) Represent the schedule as a data structure instead of a string.
    Iso8601Expressions.parse(schedule, scheduleTimeZone) match {
      case Some((rec, start, per)) =>
        if (rec == -1)
          Some(new JobSchedule(Iso8601Expressions.create(rec, start.plus(per), per), jobName,
            scheduleTimeZone))
        else if (rec > 0)
          Some(new JobSchedule(Iso8601Expressions.create(rec - 1, start.plus(per), per), jobName,
            scheduleTimeZone))
        else
          None
      case None =>
        None
    }
}
