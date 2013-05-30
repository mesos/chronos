package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.{MesosStatePersistenceStore, PersistenceStore}
import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._
import com.twitter.common.zookeeper.ZooKeeperClient

class JobSchedulerSpec extends SpecificationWithJUnit with Mockito {

  //TODO(FL): Write more specs for the REST framework.
  "JobScheduler" should {
    "Construct a task for a given time when the schedule is within epsilon" in {
      val epsilon = Minutes.minutes(1).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      val singleJobStream = new ScheduleStream("R1/2012-01-01T00:00:01.000Z/PT1M", jobName)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val horizon = Minutes.minutes(10).toPeriod
      //TODO(FL): Mock the DispatchQueue here and below
      val schedules = new JobScheduler(horizon, null, mockGraph, mock[PersistenceStore])
      val (task1, stream1) = schedules.next(new DateTime("2012-01-01T00:00:00.000Z"), singleJobStream)
      task1.get.due must beEqualTo(DateTime.parse("2012-01-01T00:00:01.000Z"))
      val (task2, stream2) = schedules.next(new DateTime("2012-01-01T00:01:00.000Z"), stream1.get)
      task2 must beNone
      stream2 must beNone
    }

    "Ignore a task that has been due past epsilon" in {
      val epsilon = Minutes.minutes(1).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      val singleJobStream = new ScheduleStream("R1/2012-01-01T00:00:01.000Z/PT1M", jobName)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val horizon = Minutes.minutes(10).toPeriod
      val schedules = new JobScheduler(horizon, null, mockGraph, mock[PersistenceStore])
      val (task1, stream1) = schedules.next(new DateTime("2012-01-01T00:01:01.000Z"), singleJobStream)
      task1 must beNone
      stream1 must beNone
    }

    "Get an empty stream if no-op schedule is given" in {
      val epsilon = Minutes.minutes(1).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      val singleJobStream = new ScheduleStream("R0/2012-01-01T00:00:01.000Z/PT1M", jobName)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val horizon = Minutes.minutes(10).toPeriod
      val schedules = new JobScheduler(horizon, null, mockGraph, mock[PersistenceStore])
      val (task1, stream1) = schedules.next(new DateTime("2012-01-01T00:01:01.000Z"), singleJobStream)
      task1 must beNone
      stream1 must beNone
    }

    "Old schedule stream is removed" in {
      val epsilon = Minutes.minutes(1).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val singleJobStream = new ScheduleStream("R1/2012-01-01T00:00:01.000Z/PT1M", jobName)

      val horizon = Minutes.minutes(10).toPeriod
      val scheduler = new JobScheduler(horizon, null, mockGraph, mock[PersistenceStore])
      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-02T00:01:01.000Z"), List(singleJobStream))
      newScheduleStreams.size must_== 0
    }

    //"This is really not a unit test but an integration test!
    "Old schedule streams are removed but newer ones are kept" in {
      val epsilon = Seconds.seconds(20).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      //2012-01-01T00:00:00.000 -> 2012-01-01T00:09:00.000
      val jobStream = new ScheduleStream("R10/2012-01-01T00:00:00.000Z/PT1M", jobName)
      // 1st planned invocation @ 2012-01-01T00:00:00.000Z (missed)
      // 2nd planned invocation @ 2012-01-01T00:01:00.000Z (executed)
      // 3rd planned invocation @ 2012-01-01T00:02:00.000Z (scheduled)
      // 4th planned invocation @ 2012-01-01T00:03:00.000Z (scheduled)
      // 5th planned invocation @ 2012-01-01T00:04:00.000Z (scheduled)
      // 6th planned invocation @ 2012-01-01T00:05:00.000Z (scheduled)
      // 7th planned invocation @ 2012-01-01T00:06:00.000Z (scheduled)
      // 8th planned invocation @ 2012-01-01T00:07:00.000Z (ahead of schedule horizon)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val mockGraph = mock[JobGraph]
      val mockPersistenceStore = mock[PersistenceStore]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val scheduler = new JobScheduler(horizon, mockTaskManager, mockGraph, mockPersistenceStore)
      // First one passed, next invocation is 01:01 (b/c of 20 second epsilon)
      // Horizon is 5 minutes, so lookforward until 00:06:01.000Z
      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-01T00:01:01.000Z"), List(jobStream))
      val (isoExpr, _) = newScheduleStreams(0).head()
      val (_, date, _) = Iso8601Expressions.parse(isoExpr)

      date must_== DateTime.parse("2012-01-01T00:07:00.000Z")
      there were 6.times(mockTaskManager).scheduleDelayedTask(any[ScheduledTask], anyLong, any[Boolean])
    }

    "Future task beyond time-horizon should not be scheduled" in {
      val epsilon = Seconds.seconds(60).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)

      val jobStream = new ScheduleStream("R10/2012-01-10T00:00:00.000Z/PT1M", jobName)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val horizon = Minutes.minutes(60).toPeriod
      val scheduler = new JobScheduler(horizon, null, mockGraph, mock[PersistenceStore])
      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-01T00:01:01.000Z"), List(jobStream))
      val (isoExpr, _) = newScheduleStreams(0).head()
      val (_, date, _) = Iso8601Expressions.parse(isoExpr)

      date must_== DateTime.parse("2012-01-10T00:00:00.000Z")
    }

    "Multiple tasks must be scheduled if they're within epsilon and before time-horizon" in {
      val epsilon = Minutes.minutes(5).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)
      val jobStream = new ScheduleStream("R60/2012-01-01T00:00:00.000Z/PT1S", jobName)

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val horizon = Minutes.minutes(1).toPeriod
      val mockTaskManager = mock[TaskManager]

      val scheduler = new JobScheduler(horizon, mockTaskManager, mockGraph, mock[PersistenceStore])

      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-01T00:01:00.000Z"), List(jobStream))
      newScheduleStreams.size must beEqualTo(0)
      there were 60.times(mockTaskManager).scheduleDelayedTask(any[ScheduledTask], anyLong, any[Boolean])
    }

    "Infinite task must be scheduled" in {
      val epsilon = Seconds.seconds(60).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "", name = jobName, command = "", epsilon = epsilon)


      val jobStream = new ScheduleStream("R/2012-01-01T00:00:00.000Z/PT1M", jobName)

      val horizon = Seconds.seconds(30).toPeriod
      val mockTaskManager = mock[TaskManager]

      val mockGraph = mock[JobGraph]
      mockGraph.lookupVertex(jobName).returns(Some(job1))

      val scheduler = new JobScheduler(horizon, mockTaskManager, mockGraph, mock[PersistenceStore])
      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-01T00:01:01.000Z"), List(jobStream))

      there was one(mockTaskManager).scheduleDelayedTask(any[ScheduledTask], any[Long], any[Boolean])
    }

    //TODO(FL): Write test that ensures that other tasks don't cause a stackoverflow

    "Job scheduler sans streams is empty" in {
      val scheduler = new JobScheduler(Period.hours(1), null, null,
        mock[PersistenceStore])
      val newScheduleStreams = scheduler.iteration(DateTime.parse("2012-01-01T00:01:00.000Z"), List())
      newScheduleStreams.size must beEqualTo(0)
    }
// TODO(FL): The interfaces have changed, this unit test needs to be added back in!
//        "New schedules can be added to JobScheduler" in {
//          val horizon = Seconds.seconds(30).toPeriod
//          val queue = mock[DispatchQueue]
//          val mockTaskManager = mock[TaskManager]
//          val jobGraph = mock[JobGraph]
//          val scheduler = new JobScheduler(horizon, queue, mockTaskManager, jobGraph, mock[PersistenceStore])
//          val newScheduleStreams = scheduler.(DateTime.parse("2012-01-01T00:00:00.000Z"), List())
//          newScheduleStreams.size must beEqualTo(0)
//          val newScheduler1 = scheduler.addSchedule("R1/2012-01-01T01:00:00.000Z/PT1M", new BaseJob("foo", Period.minutes(5)))
//          val newScheduler2 = newScheduler1.addSchedule("R1/2012-01-01T02:00:00.000Z/PT1M", new BaseJob("bar", Period.minutes(5)))
//          val updatedScheduleStreams = newScheduler2.checkAndSchedule(DateTime.parse("2012-01-01T00:01:00.000Z"))
//          updatedScheduleStreams.size must beEqualTo(0)
//          //TODO(FL): Implement a test verifying that the jobs have launched
//          //newScheduler2.numberOfScheduledJobs.get() must_== 2
//        }

  }

  "Removing tasks must also remove the streams" in {
    val epsilon = Seconds.seconds(60).toPeriod
    val job1 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "FOO", "CMD", epsilon)
    val job2 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "BAR", "CMD", epsilon)

    val horizon = Seconds.seconds(30).toPeriod
    val mockTaskManager = mock[TaskManager]
    val jobGraph = mock[JobGraph]
    val store = mock[PersistenceStore]
    store.getTaskIds(Some(anyString)).returns(List())

    jobGraph.lookupVertex(job1.name).returns(Some(job1))
    jobGraph.lookupVertex(job2.name).returns(Some(job2))
    jobGraph.getChildren(job2.name).returns(List())

    val scheduler = new JobScheduler(horizon, mockTaskManager, jobGraph, store)

    scheduler.leader.set(true)
    scheduler.registerJob(job1, false, DateTime.parse("2012-01-01T00:00:05.000Z"))
    scheduler.registerJob(job2, false, DateTime.parse("2012-01-01T00:00:10.000Z"))

    val res1 : List[ScheduleStream] = scheduler.iteration(DateTime.parse("2012-01-01T00:00:00.000Z"), scheduler.streams)

    scheduler.deregisterJob(job2, false)

    val res2 : List[ScheduleStream] = scheduler.iteration(DateTime.parse("2012-01-01T00:05:00.000Z"), scheduler.streams)
    res2.size must_== 1
    res2(0).jobName must_==job1.name
  }

  "Job scheduler persists job state after runs" in {
    val store = mock[PersistenceStore]
    val epsilon = Seconds.seconds(1).toPeriod
    val jobName = "FOO"
    val jobCmd = "BARCMD"

    val job1 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1S", jobName , jobCmd, epsilon)
    val mockGraph = mock[JobGraph]
    mockGraph.lookupVertex(job1.name).returns(Some(job1))

    val jobStream = new ScheduleStream("R/2012-01-01T00:00:00.000Z/PT1S", jobName)
    val scheduler = new JobScheduler(Period.hours(1), mock[TaskManager], mockGraph, store)

    val startTime = DateTime.parse("2012-01-01T00:00:00.000Z")
    var t: DateTime = startTime
    var stream = scheduler.iteration(startTime, List(jobStream))
    t = t.plus(Period.millis(1).toPeriod)
    stream = scheduler.iteration(t, stream)
    val job2 = new ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1S", jobName , jobCmd, epsilon, 0)
    there was one(store).persistJob(job2)
    there was one(mockGraph).replaceVertex(job1, job2)
  }

  "Recursive removing of jobs works" in {
    val epsilon = Seconds.seconds(60).toPeriod
    val job1 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "job1", "CMD", epsilon)
    val job2 = new DependencyBasedJob(Set("job1"), name = "job2", command = "CMD")
    val job3 = new DependencyBasedJob(Set("job2"), name = "job3", command = "CMD")
    val job4 = new DependencyBasedJob(Set("job3"), name = "job4", command = "CMD")
    val job5 = new DependencyBasedJob(Set("job4"), name = "job5", command = "CMD")

    val mockTaskManager = mock[TaskManager]
    val jobGraph = new JobGraph
    val store = mock[PersistenceStore]
    store.getTaskIds(Some(anyString)).returns(List())

    val scheduler = new JobScheduler(epsilon, mockTaskManager, jobGraph, store)

    val startTime = DateTime.parse("2012-01-01T00:00:00.000Z")
    scheduler.leader.set(true)
    scheduler.registerJob(job1, true, startTime)
    scheduler.registerJob(job2, true, startTime)
    scheduler.registerJob(job3, true, startTime)
    scheduler.registerJob(job4, true, startTime)
    scheduler.registerJob(job5, true, startTime)

    scheduler.deregisterJob(job3, true, true)

    scheduler.jobGraph.lookupVertex("job3") must_== None
    scheduler.jobGraph.lookupVertex("job4") must_== None
    scheduler.jobGraph.lookupVertex("job5") must_== None

    there were three(store).removeJob(any[BaseJob])
  }

  "Removing a dependency of another job throws an exception without the force method" in {
    val epsilon = Seconds.seconds(60).toPeriod
    val job1 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "job1", "CMD", epsilon)
    val job2 = new DependencyBasedJob(Set("job1"), name = "job2", command = "CMD")
    val job3 = new DependencyBasedJob(Set("job2"), name = "job3", command = "CMD")
    val job4 = new DependencyBasedJob(Set("job3"), name = "job4", command = "CMD")
    val job5 = new DependencyBasedJob(Set("job4"), name = "job5", command = "CMD")

    val mockTaskManager = mock[TaskManager]
    val jobGraph = new JobGraph
    val store = mock[PersistenceStore]
    store.getTaskIds(Some(anyString)).returns(List())


    val scheduler = new JobScheduler(epsilon, mockTaskManager, jobGraph, store)

    val startTime = DateTime.parse("2012-01-01T00:00:00.000Z")
    scheduler.leader.set(true)
    scheduler.registerJob(job1, true, startTime)
    scheduler.registerJob(job2, true, startTime)
    scheduler.registerJob(job3, true, startTime)
    scheduler.registerJob(job4, true, startTime)
    scheduler.registerJob(job5, true, startTime)

    scheduler.deregisterJob(job3, true, false) must throwA[IllegalArgumentException]
  }

  "Removing a dependency of another job removes the parent properly from the list of parents" in {
    val epsilon = Seconds.seconds(60).toPeriod
    val job1 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "job1", "CMD", epsilon)
    val job2 = new ScheduleBasedJob("R/2012-01-01T00:00:00.000Z/PT1M", "job2", "CMD", epsilon)
    val job3 = new DependencyBasedJob(Set("job2"), name = "job3", command = "CMD")
    val job4 = new DependencyBasedJob(Set("job1"), name = "job4", command = "CMD")
    val job5 = new DependencyBasedJob(Set("job1", "job3"), name = "job5", command = "CMD")

    val mockTaskManager = mock[TaskManager]
    val jobGraph = new JobGraph
    val store = mock[PersistenceStore]
    store.getTaskIds(Some(anyString)).returns(List())

    val scheduler = new JobScheduler(epsilon, mockTaskManager, jobGraph, store)

    val startTime = DateTime.parse("2012-01-01T00:00:00.000Z")
    scheduler.leader.set(true)
    scheduler.registerJob(job1, true, startTime)
    scheduler.registerJob(job2, true, startTime)
    scheduler.registerJob(job3, true, startTime)
    scheduler.registerJob(job4, true, startTime)
    scheduler.registerJob(job5, true, startTime)

    scheduler.deregisterJob(job2, true, true)

    scheduler.jobGraph.lookupVertex("job2")  must_== None
    scheduler.jobGraph.lookupVertex("job3")  must_== None
    scheduler.jobGraph.lookupVertex("job5").get.asInstanceOf[DependencyBasedJob].parents.size must_== 1
    scheduler.jobGraph.lookupVertex("job5").get.asInstanceOf[DependencyBasedJob].parents must_== Set("job1")
  }

  "Missed executions have to be skipped" in {
    val epsilon = Seconds.seconds(60).toPeriod
    val job1 = new ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD", epsilon)

    val mockTaskManager = mock[TaskManager]
    val jobGraph = new JobGraph
    val mockPersistenceStore = mock[PersistenceStore]

    val scheduler = new JobScheduler(epsilon, mockTaskManager, jobGraph, mockPersistenceStore)

    val startTime = DateTime.parse("2012-01-03T00:00:00.000Z")
    scheduler.leader.set(true)
    scheduler.registerJob(job1, true, startTime)

    val newStreams = scheduler.iteration(startTime, scheduler.streams)
    newStreams(0).schedule must_== "R2/2012-01-04T00:00:00.000Z/P1D"
  }
}
