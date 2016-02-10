package org.apache.mesos.chronos.scheduler.jobs

/**
 * A stream of schedules.
 * Calling tail will return a clipped schedule.
 * The schedule consists of a string representation of an ISO8601 expression as well as a BaseJob.
 * @author Florian Leibert (flo@leibert.de)
 */
class ScheduleStream(val jobName: String, val schedule: Schedule) {

  def head: (String, Schedule) = (jobName, schedule)

  /**
   * Returns a clipped schedule.
   * @return
   */
  def tail: Option[ScheduleStream] =
    schedule.next.map(s => new ScheduleStream(jobName, s))

  override def toString = s"ScheduleStream{ job: $jobName [$schedule] }"
}
