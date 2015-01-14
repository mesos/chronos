
package com.airbnb.scheduler.jobs

import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._

class TaskUtilsSpec extends SpecificationWithJUnit with Mockito {

  "One should be able to parse the generated taskId" in {
    val attempt = 2
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()
    val taskId = TaskUtils.getTaskId(job, now, attempt)
    val (jobName2, due2, attempt2) = TaskUtils.parseTaskId(taskId)
    jobName2 must_== job.name
    due2 must_== now.getMillis
    attempt2 must_== attempt
  }

  "The default attempt number should be zero" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()
    val taskId = TaskUtils.getTaskId(job, now)
    val (jobName2, due2, attempt2) = TaskUtils.parseTaskId(taskId)
    jobName2 must_== job.name
    due2 must_== now.getMillis
    attempt2 must_== 0
  }

  "The generated taskId should be " in {
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()
    val taskId = TaskUtils.getTaskId(job, now)
    TaskUtils.isValidVersion(taskId)
  }

  "The old task id should be valid " in {
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()
    val taskId = TaskUtils.getTaskIdOld(job, now)
    TaskUtils.isValidVersion(taskId)
  }

  "One should be able to parse the generated taskId in old format" in {
    val attempt = 2
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()
    val taskId = TaskUtils.getTaskIdOld(job, now, attempt)
    val (jobName2, due2, attempt2) = TaskUtils.parseTaskId(taskId)
    jobName2 must_== job.name
    due2 must_== now.getMillis
    attempt2 must_== attempt
    TaskUtils.isValidVersion(taskId)
  }

}