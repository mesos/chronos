package com.airbnb.scheduler.jobs
import org.apache.mesos.Protos.TaskStatus

/**
 * Created by wensheng_hua on 11/18/14.
 */
trait JobStats {

  def jobStarted(job: BaseJob, taskStatus: TaskStatus, attempt: Int)

  def jobFinished(job: BaseJob, taskStatus: TaskStatus, attempt: Int)

  def jobFailed(job: BaseJob, taskStatus: TaskStatus, attempt: Int)
}
