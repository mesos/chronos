package org.apache.mesos.chronos.scheduler.jobs

import java.util.logging.Logger

import org.apache.mesos.Protos.TaskStatus
import org.joda.time.DateTime

trait JobEvent
case class JobQueued(job: BaseJob, taskId: String, attempt: Int) extends JobEvent
case class JobSkipped(job: BaseJob, dateTime: DateTime) extends JobEvent
case class JobStarted(job: BaseJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobFinished(job: BaseJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
// Either a job name or job object, depending on whether the Job still exists
case class JobFailed(job: Either[String, BaseJob], taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobDisabled(job: BaseJob, cause: String) extends JobEvent
case class JobRetriesExhausted(job: BaseJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobRemoved(job: BaseJob) extends JobEvent
// For now, Chronos does not expire tasks once they are queued
//case class JobExpired(job: BaseJob, taskId: String, attempt: Int) extends JobEvent

object JobsObserver {
  type Observer = PartialFunction[JobEvent, Unit]
  private[this] val log = Logger.getLogger(getClass.getName)

  def composite(observers: List[Observer]): Observer = {
    case event => observers.foreach(observer => observer.lift.apply(event).orElse {
      log.info(s"$observer does not handle $event")
      Some(Unit)
    })
  }
}
