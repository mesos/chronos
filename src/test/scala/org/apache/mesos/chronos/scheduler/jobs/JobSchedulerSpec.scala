package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

import scala.util.Random

class JobSchedulerSpec extends SpecificationWithJUnit with Mockito {
  "JobScheduler" should {
    "Run the correct job" in {
      val job1 =
        ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")
      val job2 = ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D",
                                  "job2",
                                  "CMD",
                                  disabled = true)
      val futureDate = DateTime.now().plusYears(1)
      val job3 = ScheduleBasedJob(
        s"R5/${ISODateTimeFormat.dateTime().print(futureDate)}/P1D",
        "job3",
        "CMD")

      val jobGraph = mock[JobGraph]
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      scheduler.leader.set(true)
      val scheduledJobs = scheduler.registerJobs(List(job1, job2, job3))
      val (jobsToRun, jobsNotToRun) = scheduler.getJobsToRun(scheduledJobs)
      scheduler.runJobs(jobsToRun)

      jobsToRun.size must_== 1
      jobsNotToRun.size must_== 2

      jobsNotToRun.head.name must beEqualTo("job2")
      jobsNotToRun(1).name must beEqualTo("job3")

      there was one(jobGraph).addVertex(job1) andThen
        one(jobGraph).addVertex(job2) andThen
        one(jobGraph).addVertex(job3)

      there was one(mockTaskManager).enqueue(endWith(":job1:"), anyBoolean)
    }

    "Register dependent job correctly" in {
      val job1 =
        ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")
      val job2 = DependencyBasedJob(Set("job1"), "job2", "CMD")

      val jobGraph = new JobGraph
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      scheduler.leader.set(true)
      val jobs = List(job1, job2)
      scheduler.registerJobs(jobs)

      jobGraph.lookupVertex("job1") mustEqual Some(job1)
      jobGraph.lookupVertex("job2") mustEqual Some(job2)
      jobGraph.parentJobsOption(job2) mustEqual Some(List(job1))
    }

    "Register jobs in correct order and enqueue them" in {
      val job1 =
        ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")
      val job2 =
        ScheduleBasedJob("R5/2012-02-01T00:00:00.000Z/P1D", "job2", "CMD")
      val job3 =
        ScheduleBasedJob("R5/2012-03-01T00:00:00.000Z/P1D", "job3", "CMD")
      val futureDate = DateTime.now().plusYears(1)
      val job4 = ScheduleBasedJob(
        s"R5/${ISODateTimeFormat.dateTime().print(futureDate)}/P1D",
        "job4",
        "CMD")

      val jobGraph = mock[JobGraph]
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      scheduler.leader.set(true)
      val scheduledJobs =
        scheduler.registerJobs(Random.shuffle(List(job1, job2, job3, job4)))
      val (jobsToRun, jobsNotToRun) = scheduler.getJobsToRun(scheduledJobs)
      scheduler.runJobs(jobsToRun)

      jobsToRun.size must_== 3
      jobsNotToRun.size must_== 1

      jobsToRun.head.name must beEqualTo("job1")
      jobsToRun(1).name must beEqualTo("job2")
      jobsToRun(2).name must beEqualTo("job3")
      jobsNotToRun.head.name must beEqualTo("job4")

      there was one(jobGraph).addVertex(job1) andThen
        one(jobGraph).addVertex(job2) andThen
        one(jobGraph).addVertex(job3) andThen
        one(jobGraph).addVertex(job4)
      there were 3.times(mockTaskManager).enqueue(anyString, anyBoolean)
      there was one(mockTaskManager)
        .enqueue(endingWith(":job3:"), anyBoolean) andThen
        one(mockTaskManager).enqueue(endingWith(":job2:"), anyBoolean) andThen
        one(mockTaskManager).enqueue(endingWith(":job1:"), anyBoolean)
    }

    "Compute the correct amount of sleep time" in {
      val job1 =
        ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")
      val job2 =
        ScheduleBasedJob("R5/2012-02-01T00:00:00.000Z/P1D", "job2", "CMD")
      val job3 = ScheduleBasedJob("R5/2012-03-01T00:00:00.000Z/P1D",
                                  "job3",
                                  "CMD",
                                  disabled = true)
      val futureDate1 = DateTime.now(DateTimeZone.UTC).plusHours(1)
      val job4 = ScheduleBasedJob(
        s"R5/${ISODateTimeFormat.dateTime().print(futureDate1)}/P1D",
        "job4",
        "CMD")
      val futureDate2 = DateTime.now(DateTimeZone.UTC).plusHours(2)
      val job5 = ScheduleBasedJob(
        s"R5/${ISODateTimeFormat.dateTime().print(futureDate2)}/P1D",
        "job5",
        "CMD")

      val jobGraph = mock[JobGraph]
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      var nanos =
        scheduler.nanosUntilNextJob(List(job1, job2, job3, job4, job5))
      nanos must_== 0

      nanos = scheduler.nanosUntilNextJob(List(job2, job3, job4, job5))
      nanos must_== 0

      nanos = scheduler.nanosUntilNextJob(List(job3, job4, job5))
      nanos must beCloseTo(60 * 60 * 1000000000l, 5000000000l) // within 5s

      nanos = scheduler.nanosUntilNextJob(List(job4, job5))
      nanos must beCloseTo(60 * 60 * 1000000000l, 5000000000l) // within 5s

      nanos = scheduler.nanosUntilNextJob(List(job5))
      nanos must beCloseTo(2 * 60 * 60 * 1000000000l, 5000000000l) // within 5s
    }

    "A parent job succeeds and child is enqueued" in {
      val job1 =
        ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")
      val job2 = DependencyBasedJob(Set("job1"), "job2", "CMD")
      val job3 =
        DependencyBasedJob(Set("job1"), "job3", "CMD", disabled = true)
      val job4 = DependencyBasedJob(Set("job2"), "job4", "CMD")

      val jobGraph = new JobGraph
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      scheduler.leader.set(true)
      val jobs = List(job1, job2, job3, job4)
      scheduler.registerJobs(jobs)

      jobGraph.lookupVertex("job1") mustEqual Some(job1)
      jobGraph.lookupVertex("job2") mustEqual Some(job2)
      jobGraph.lookupVertex("job3") mustEqual Some(job3)
      jobGraph.lookupVertex("job4") mustEqual Some(job4)
      jobGraph.parentJobsOption(job2) mustEqual Some(List(job1))
      jobGraph.parentJobsOption(job3) mustEqual Some(List(job1))
      jobGraph.parentJobsOption(job4) mustEqual Some(List(job2))

      scheduler.markJobSuccessAndFireOffDependencies("job1")
      scheduler.markJobSuccessAndFireOffDependencies("job2")

      there was one(mockTaskManager)
        .enqueue(endWith(":job2:"), anyBoolean) andThen
        no(mockTaskManager).enqueue(endWith(":job3:"), anyBoolean) andThen
        one(mockTaskManager).enqueue(endWith(":job4:"), anyBoolean)
    }

    "A single repitition job doesn't fail" in {
      val job1 =
        ScheduleBasedJob("R1/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")

      val jobGraph = new JobGraph
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler =
        MockJobUtils.mockScheduler(mockTaskManager, jobGraph, persistenceStore)

      val job2 = scheduler.getNewRunningJob(job1)

      job1.name must_== job2.name
    }
  }
}
