package com.airbnb.scheduler.jobs

import java.util.logging.{Handler, LogRecord, Logger}

import org.apache.mesos.Protos
import org.joda.time.Minutes
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

/**
 * Created by wensheng_hua on 11/19/14.
 */
class JobStatsCsvSpec extends SpecificationWithJUnit with Mockito {

  "Job status should be correctly logged" in {
    val state  = Protos.TaskState.TASK_RUNNING
    val taskId = Protos.TaskID.newBuilder()
    val taskIdStr = "task1"
    taskId.setValue(taskIdStr)

    val taskStatus = Protos.TaskStatus.newBuilder()
    taskStatus.setTaskId(taskId.build())
    taskStatus.setState(state)
    val slaveId = Protos.SlaveID.newBuilder()
    slaveId.setValue("slave")
    taskStatus.setSlaveId(slaveId)
    val taskMessage = "message"
    taskStatus.setMessage(taskMessage)
    val taskStat: Protos.TaskStatus = taskStatus.build()
    val attempt = 1

    val epsilon = Minutes.minutes(20).toPeriod
    val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
      name = "job1", owner = "me", command = "fooo", epsilon = epsilon, disabled=false)
    val job5 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled=false)

    val handler= mock[Handler]
    val logger = Logger.getLogger("com.airbnb.scheduler.jobs.JobStatsCsv")
    logger.setUseParentHandlers(false)
    logger.addHandler(handler)
    val jobStatsCsv = new JobStatsCsv
    jobStatsCsv.jobStarted(job1, taskStat, attempt)
    jobStatsCsv.jobStarted(job5, taskStat, attempt)
    jobStatsCsv.jobFinished(job1, taskStat, attempt)
    jobStatsCsv.jobFailed(job5, taskStat, attempt)

    val record = capture[LogRecord]
    there was 4.times(handler).publish(record)
    logger.removeHandler(handler)

    val message = record.values.get(0).getMessage
    val parts = message.split(",")
    parts.size must_==(11)
    parts(0) must_== taskIdStr
    parts(2) must_== job1.name
    parts(3) must_== job1.owner
    parts(4) must_== job1.schedule
    parts(5) must_== ""
    parts(6) must_== Protos.TaskState.TASK_RUNNING.toString
    parts(7) must_== taskStat.getSlaveId.getValue
    parts(8) must_== ""
    parts(9) must_== attempt.toString
    parts(10) must_== "FALSE"

    val message1 = record.values.get(1).getMessage
    val parts1 = message1.split(",")
    parts1.size must_==(11)
    parts1(0) must_== taskIdStr
    parts1(2) must_== job5.name
    parts1(3) must_== ""
    parts1(4) must_== ""
    parts1(5) must_== "job1:job2:job3"
    parts1(6) must_== Protos.TaskState.TASK_RUNNING.toString
    parts1(7) must_== taskStat.getSlaveId.getValue
    parts1(8) must_== ""
    parts1(9) must_== attempt.toString
    parts1(10) must_== "FALSE"

    val message2 = record.values.get(2).getMessage
    val parts2 = message2.split(",")
    parts2(0) must_== taskIdStr
    parts2(2) must_== job1.name
    parts2(3) must_== job1.owner
    parts2(4) must_== job1.schedule
    parts2(5) must_== ""
    parts2(6) must_== Protos.TaskState.TASK_RUNNING.toString
    parts2(7) must_== taskStat.getSlaveId.getValue
    parts2(8) must_== ""
    parts2(9) must_== attempt.toString
    parts2(10) must_== "FALSE"

    val message3 = record.values.get(3).getMessage
    val parts3 = message3.split(",")
    parts3.size must_==(11)
    parts3(0) must_== taskIdStr
    parts3(2) must_== job5.name
    parts3(3) must_== ""
    parts3(4) must_== ""
    parts3(5) must_== "job1:job2:job3"
    parts3(6) must_== Protos.TaskState.TASK_RUNNING.toString
    parts3(7) must_== taskStat.getSlaveId.getValue
    parts3(8) must_== taskStat.getMessage
    parts3(9) must_== attempt.toString
    parts3(10) must_== "TRUE"
  }

}