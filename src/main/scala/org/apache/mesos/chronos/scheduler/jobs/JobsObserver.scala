package org.apache.mesos.chronos.scheduler.jobs

import java.util.logging.Logger

import org.apache.mesos.Protos.TaskStatus
import org.joda.time.DateTime

trait JobEvent
case class JobQueued(job: StoredJob, taskId: String, attempt: Int) extends JobEvent
case class JobSkipped(job: StoredJob, dateTime: DateTime) extends JobEvent
case class JobStarted(job: StoredJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobFinished(job: StoredJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
// Either a job name or job object, depending on whether the Job still exists
case class JobFailed(job: Either[String, StoredJob], taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobDisabled(job: StoredJob, cause: String) extends JobEvent
case class JobRetriesExhausted(job: StoredJob, taskStatus: TaskStatus, attempt: Int) extends JobEvent
case class JobRemoved(job: StoredJob) extends JobEvent
// This event is fired when job is disabled (e.g. due to recurrence going to 0) and its queued tasks are purged
case class JobExpired(job: StoredJob, taskId: String) extends JobEvent

object JobsObserver {
  type Observer = PartialFunction[JobEvent, Unit]
  private[this] val log = Logger.getLogger(getClass.getName)

  def composite(observers: List[Observer]): Observer = {
    case event => observers.foreach(observer => observer.lift.apply(event).orElse {
      log.info(s"$observer does not handle $event")
      Some(Unit)
    })
  }

  def withName(observer: Observer, name: String): Observer = new Observer {
    override def isDefinedAt(event: JobEvent) = observer.isDefinedAt(event)
    override def apply(event: JobEvent): Unit = observer.apply(event)
    override def toString(): String = name
  }
}
