package com.airbnb.scheduler.jobs

import org.joda.time.{DateTimeZone, Hours, Minutes, DateTime}
import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.PersistenceStore
import org.specs2.mock.Mockito
import org.specs2.mutable._
import java.util.concurrent.ScheduledThreadPoolExecutor
import com.google.common.util.concurrent.MoreExecutors
import com.codahale.metrics.MetricRegistry
import com.airbnb.scheduler.mesos.MesosDriverFactory
import com.airbnb.scheduler.api.{Iso8601JobResource, DependentJobResource}
import org.joda.time.format.ISODateTimeFormat

class JobSchedulerIntegrationTest extends SpecificationWithJUnit with Mockito {

  "JobScheduler" should {
    "A job creates a failed task and then a successful task from a synchronous job" in {
      val epsilon = Hours.hours(2).toPeriod
      val job1 = new ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "", "job1", "CMD", epsilon)

      val exec = MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(1))

      val jobGraph = new JobGraph
      val persistenceStore = mock[PersistenceStore]
      val taskManager = new TaskManager(exec, persistenceStore, jobGraph, mesosDriver = mock[MesosDriverFactory], registry = mock[MetricRegistry])

      val scheduler = new JobScheduler(epsilon, taskManager, jobGraph, persistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      val startTime = DateTime.parse("2012-01-01T01:00:00.000Z")
      scheduler.leader.set(true)
      scheduler.registerJob(job1, true, startTime)

      val newStreams = scheduler.iteration(startTime, scheduler.streams)
      newStreams(0).schedule must_== "R4/2012-01-02T00:00:00.000Z/P1D"
      taskManager.queues(1).size must_== 1

      val taskId1 = taskManager.queues(1).poll

      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, startTime, 0))

      taskManager.queues(1).size must_== 1

      val taskId2 = taskManager.queues(1).poll

      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, startTime, 0))
      there was one(persistenceStore)
        .persistJob(new ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "", "job1", "CMD", epsilon))

      there was one(persistenceStore)
        .persistJob(new ScheduleBasedJob("R4/2012-01-02T00:00:00.000Z/P1D", "", "job1", "CMD", epsilon))
    }

    "Executing a job updates the job counts and errors" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val jobName = "FOO"
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = jobName, command = "fooo", epsilon = epsilon, retries = 0)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]
      val mockJobStats = mock[JobStats]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mockJobStats)
      scheduler.leader.set(true)
      scheduler.registerJob(job1, true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.run(() => { DateTime.parse("2012-01-01T00:05:01.000Z")})

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
      val job2 = graph.lookupVertex(jobName).get
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
      val job3 = graph.lookupVertex(jobName).get
      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))

      graph.lookupVertex(jobName).get.successCount must_== 2
      graph.lookupVertex(jobName).get.errorCount must_== 1

      there was one(mockJobStats).jobFinished(job1, TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0)
      there was one(mockJobStats).jobFinished(job2, TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0)
      there was one(mockJobStats).jobFailed(job3, TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0)
    }

    "Tests that a disabled job does not run and does not execute dependant children." in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled=true)
      val job2 = new DependencyBasedJob(Set("job1"), name = "job2", command = "CMD", disabled=true)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      scheduler.leader.set(true)
      scheduler.registerJob(job1, true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.registerJob(job2, true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.run(() => { DateTime.parse("2012-01-01T00:05:01.000Z")})
      /*
            scheduler.handleFinishedTask(TaskUtils.getTaskId(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
            scheduler.handleFinishedTask(TaskUtils.getTaskId(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
            scheduler.handleFailedTask(TaskUtils.getTaskId(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
      */
      graph.lookupVertex("job1").get.successCount must_== 0
      graph.lookupVertex("job1").get.errorCount must_== 0
      graph.lookupVertex("job2").get.successCount must_== 0
      graph.lookupVertex("job2").get.errorCount must_== 0
    }

    "Tests that dependent jobs runs when they should" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled=false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled=false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled=false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled=false)
      val job5 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled=false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      scheduler.leader.set(true)
      val date = DateTime.parse("2011-01-01T00:05:01.000Z")
      scheduler.registerJob(job1, true, date)
      scheduler.registerJob(job2, true, date)
      scheduler.registerJob(job3, true, date)
      scheduler.registerJob(job4, true, date)
      scheduler.registerJob(job5, true, date)
      scheduler.run(() => { date })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0
    }

    "Tests that dependent jobs runs when they should after changing the jobgraph" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled=false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled=false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled=false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled=false)
      val job5_1 = new DependencyBasedJob(Set("job1", "job2"), name = "job5", command = "CMD", disabled=false)
      val job5_2 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled=false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      scheduler.leader.set(true)
      val date = DateTime.parse("2011-01-01T00:05:01.000Z")
      scheduler.registerJob(job1, true, date)
      scheduler.registerJob(job2, true, date)
      scheduler.registerJob(job3, true, date)
      scheduler.registerJob(job4, true, date)
      scheduler.registerJob(job5_1, true, date)

      scheduler.run(() => { date })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_1, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_1, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      val jobResource = new DependentJobResource(jobScheduler = scheduler, jobGraph = graph)
      jobResource.handleRequest(job5_2)

      scheduler.run(() => { date })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 2
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 2
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 2
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 2
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_2, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_2, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0
    }

    "Tests that complex dependent jobs run when they should" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        scheduleTimeZone = "", name = "job1", command = "fooo", epsilon = epsilon, disabled=false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        scheduleTimeZone = "", name = "job2", command = "fooo", epsilon = epsilon, disabled=false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled=false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled=false)
      val job5 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled=false)
      val job6 = new DependencyBasedJob(Set("job4", "job5", "job1"), name = "job6", command = "CMD", disabled=false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      scheduler.leader.set(true)
      val date = DateTime.parse("2011-01-01T00:05:01.000Z")
      scheduler.registerJob(job1, true, date)
      scheduler.registerJob(job2, true, date)
      scheduler.registerJob(job3, true, date)
      scheduler.registerJob(job4, true, date)
      scheduler.registerJob(job5, true, date)
      scheduler.registerJob(job6, true, date)
      scheduler.run(() => { date })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      scheduler.run(() => { date })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 2
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 2
      graph.lookupVertex("job2").get.errorCount must_== 0

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job6, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job6, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job6").get.successCount must_== 1
      graph.lookupVertex("job6").get.errorCount must_== 0
    }

    "Tests that dependent jobs run when parents are updated" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val date = DateTime.now(DateTimeZone.UTC)
      val fmt = ISODateTimeFormat.dateTime()
      val job1 = new ScheduleBasedJob(schedule = s"R/${fmt.print(date)}/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled=false)
      val job2 = new ScheduleBasedJob(schedule = s"R/${fmt.print(date)}/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled=false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled=false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled=false)
      val job5_1 = new DependencyBasedJob(Set("job1", "job2"), name = "job5", command = "CMD", disabled=false)
      val job5_2 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled=false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = new JobScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, jobMetrics = mock[JobMetrics], jobStats = mock[JobStats])
      scheduler.leader.set(true)
      scheduler.registerJob(job1, true, date)
      scheduler.registerJob(job2, true, date)
      scheduler.registerJob(job3, true, date)
      scheduler.registerJob(job4, true, date)
      scheduler.registerJob(job5_1, true, date)

      scheduler.run(() => { date })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_1, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_1, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      val jobResource = new Iso8601JobResource(jobScheduler = scheduler, jobGraph = graph)
      jobResource.handleRequest(job1)
      jobResource.handleRequest(job2)

      scheduler.run(() => { date })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 2
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 2
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_2, finishedDate, 0), false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_2, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 2
      graph.lookupVertex("job5").get.errorCount must_== 0
    }
  }
}
