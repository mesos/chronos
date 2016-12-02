package org.apache.mesos.chronos.scheduler.jobs

import org.apache.curator.framework.CuratorFramework
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.state.MesosStatePersistenceStore
import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._

class JobUtilsSpec extends SpecificationWithJUnit with Mockito {

  "Save a ScheduleBasedJob job correctly and be able to load it" in {
    val mockZKClient = mock[CuratorFramework]
    val config = new SchedulerConfiguration {}
    val store = new MesosStatePersistenceStore(mockZKClient, config)
    val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
    val job = ScheduleBasedJob(startTime, "sample-name", "sample-command")
    val mockScheduler = mock[JobScheduler]

    store.persistJob(job)
    JobUtils.loadJobs(store)

    true must beTrue
  }

  "Can skip forward a job" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val scheduledTime = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get._2

    // Ensure that this job runs today
    scheduledTime.toLocalDate must_== now.toLocalDate
  }

  "Can skip forward a job with a monthly period" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/P1M"
    val job = ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val scheduledTime = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get._2

    // Ensure that this job runs on the first of next month
    scheduledTime.isAfter(now) must beTrue
    scheduledTime.dayOfMonth().get must_== 1
  }

  "Doesn't skip forward a job" in {
    val now = new DateTime()
    val schedule = s"R/${now.plusDays(1).toDateTimeISO.toString}/PT1M"
    val job = ScheduleBasedJob(schedule, "sample-name", "sample-command")

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val scheduledTime = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get._2

    scheduledTime.toLocalDate must_== now.toLocalDate.plusDays(1)
  }

  "Doesn't skip forward a job with no period" in {
    val now = new DateTime()
    val schedule = s"R0/${now.minusDays(1).toDateTimeISO.toString}/PT0S"
    val job = ScheduleBasedJob(schedule, "sample-name", "sample-command")

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val (repeat, scheduledTime, period) = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get

    period.toStandardSeconds.getSeconds must_== 0
    scheduledTime.toLocalDate must_== now.toLocalDate.minusDays(1)
  }

  "Can get job with arguments" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/P1M"
    val arguments = "--help"
    val command = "sample-command"
    val commandWithArguments = command + " " + arguments

    val scheduledJob = ScheduleBasedJob(schedule, "sample-name", command = command)
    val dependencyJob = DependencyBasedJob(parents = Set("sample-name"), "sample-name2", command = command)
    val scheduledJobWithArguments = JobUtils.getJobWithArguments(scheduledJob, arguments)
    val dependencyJobWithArguments = JobUtils.getJobWithArguments(dependencyJob, arguments)

    scheduledJobWithArguments.command.toString must_== commandWithArguments
    dependencyJobWithArguments.command.toString must_== commandWithArguments
  }

  "Accepts a job name with periods" in {
    val jobName = "sample.name"

    JobUtils.isValidJobName(jobName)
  }
}
