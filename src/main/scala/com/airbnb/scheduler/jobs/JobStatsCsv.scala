package com.airbnb.scheduler.jobs

import java.util.logging.Logger

import org.apache.mesos.Protos.TaskStatus

class JobStatsCsv extends JobStats {
  
  val ID_INDEX: Int = 0
  val TS_INDEX: Int = 1
  val JOB_NAME_INDEX: Int = 2
  val JOB_OWNER_INDEX: Int = 3
  val JOB_SCHEDULE_INDEX: Int = 4
  val JOB_PARENT_INDEX: Int = 5
  val TASK_STATE_INDEX: Int = 6
  val SLAVE_ID_INDEX: Int = 7
  val MESSAGE_INDEX: Int = 8
  val ATTEMPT_INDEX: Int = 9
  val IS_FAILURE_INDEX: Int = 10
  val CSV_LEN: Int = 11

  val SPLITTER: String = ","
  val PARENT_SPLITTER: String = ":"
  val DEFAULT_VALUE: String = ""
  val TRUE: String = "TRUE"
  val FALSE: String = "FALSE"

  val log = Logger.getLogger(getClass.getName)

  override def jobStarted(job: BaseJob, taskStatus: TaskStatus, attempt: Int): Unit = {
    logData(jobStatString(job, taskStatus, attempt))
  }

  override def jobFailed(job: BaseJob, taskStatus: TaskStatus, attempt: Int): Unit = {
    val data = jobStatString(job, taskStatus, attempt)
    data(IS_FAILURE_INDEX) = TRUE
    data(MESSAGE_INDEX) = taskStatus.getMessage
    logData(data)
  }

  override def jobFinished(job: BaseJob, taskStatus: TaskStatus, attempt: Int): Unit = {
    logData(jobStatString(job, taskStatus, attempt))
  }

  private def logData(data: Array[String]): Unit = {
    log.info(data.mkString(SPLITTER))
  }

  private def jobStatString(job: BaseJob, taskStatus: TaskStatus, attempt: Int):Array[String] = {
    val data = new Array[String](CSV_LEN)
    data(ID_INDEX) = taskStatus.getTaskId.getValue
    data(TS_INDEX) = new java.util.Date().toString
    data(JOB_NAME_INDEX) = job.name
    data(JOB_OWNER_INDEX) = job.owner
    data(JOB_SCHEDULE_INDEX) = DEFAULT_VALUE
    data(JOB_PARENT_INDEX) = DEFAULT_VALUE
    data(TASK_STATE_INDEX) = taskStatus.getState.toString
    data(SLAVE_ID_INDEX) = taskStatus.getSlaveId.getValue
    data(MESSAGE_INDEX) = DEFAULT_VALUE
    data(ATTEMPT_INDEX) = attempt.toString
    data(IS_FAILURE_INDEX) = FALSE
    job match {
      case job: ScheduleBasedJob =>
        data(JOB_SCHEDULE_INDEX) = job.schedule.toString
      case job: DependencyBasedJob =>
        data(JOB_PARENT_INDEX) = job.parents.toSet.mkString(PARENT_SPLITTER)
    }
    return data
  }
}