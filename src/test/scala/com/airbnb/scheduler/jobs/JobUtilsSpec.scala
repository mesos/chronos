package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.state.MesosStatePersistenceStore
import org.specs2.mock._
import org.specs2.mutable._
import org.apache.zookeeper.ZooKeeper
import org.apache.curator.framework.CuratorFramework
import org.joda.time._

class JobUtilsSpec extends SpecificationWithJUnit with Mockito {

  "Save a ScheduleBasedJob job correctly and be able to load it" in {
    val mockZKClient = mock[CuratorFramework]
    val config = new SchedulerConfiguration { }
    val store = new MesosStatePersistenceStore(mockZKClient, config)
    val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(startTime, "sample-name", "sample-command")
    val mockScheduler = mock[JobScheduler]

    store.persistJob(job)
    JobUtils.loadJobs(mockScheduler, store)

    there was one(mockScheduler).registerJob(List(job), persist = true)
  }

  "Can skip forward a job" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val scheduledTime = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get._2
    
    // Ensure that this job runs today
    scheduledTime.toLocalDate() must_== now.toLocalDate()
  }
  
  "Can skip forward a job with a monthly period" in {
    val schedule = "R/2012-01-01T00:00:01.000Z/P1M"
    val job = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
    val now = new DateTime()

    // Get the schedule stream, which should have been skipped forward
    val stream = JobUtils.skipForward(job, now)
    val scheduledTime = Iso8601Expressions.parse(stream.get.schedule, job.scheduleTimeZone).get._2
    
    // Ensure that this job runs on the first of next month
    scheduledTime.isAfter(now) must beTrue
    scheduledTime.dayOfMonth().get must_== 1
  }
}
