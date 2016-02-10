package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.state.MesosStatePersistenceStore
import org.apache.curator.framework.CuratorFramework
import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._

class JobUtilsSpec extends SpecificationWithJUnit with Mockito {

  "Save a ScheduleBasedJob job correctly and be able to load it" in {
    val mockZKClient = mock[CuratorFramework]
    val config = new SchedulerConfiguration {}
    val store = new MesosStatePersistenceStore(mockZKClient, config)
    val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
    val job = new InternalScheduleBasedJob(scheduleData = Schedule.parse(startTime).get, "sample-name", "sample-command")
    val mockScheduler = mock[JobScheduler]

    store.persistJob(job)
    JobUtils.loadJobs(mockScheduler, store)

    there was one(mockScheduler).registerJob(List(job), persist = true)
  }

  "Can skip forward a job" in {
    val schedule = Schedule.parse("R/2012-01-01T00:00:01.000Z/PT1M").get
    val job = new InternalScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(new ScheduleStream("sample-name", schedule), now, job.epsilon)
    val scheduledTime = stream.get.schedule.invocationTime

    // Ensure that this job runs today
    scheduledTime.toLocalDate must_== now.toLocalDate
  }

  "Can skip forward a job with a monthly period" in {
    val schedule = Schedule.parse("R/2012-01-01T00:00:01.000Z/P1M").get
    val job = new InternalScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(new ScheduleStream("sample-name", schedule), now, job.epsilon)
    val scheduledTime = stream.get.schedule.invocationTime

    // Ensure that this job runs on the first of next month
    scheduledTime.isAfter(now) must beTrue
    scheduledTime.dayOfMonth().get must_== 1
  }

  "Can get job with arguments" in {
    val schedule = Schedule.parse("R/2012-01-01T00:00:01.000Z/P1M").get
    val arguments = "--help"
    val command = "sample-command"
    val commandWithArguments = command + " " + arguments

    val scheduledJob = new InternalScheduleBasedJob(schedule, "sample-name", command = command)
    val dependencyJob = new DependencyBasedJob(parents = Set("sample-name"), "sample-name2", command = command)
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
