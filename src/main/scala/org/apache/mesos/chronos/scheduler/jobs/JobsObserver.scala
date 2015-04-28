package org.apache.mesos.chronos.scheduler.jobs

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

trait JobsObserver {
  def onEvent(event: JobEvent)
}

object JobsObserver {
  def composite(observers: List[JobsObserver]): JobsObserver = new JobsObserver {
    override def onEvent(event: JobEvent): Unit = observers.foreach(_.onEvent(event))
  }
}
