/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.api.{ DependentJobResource, Iso8601JobResource }
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone, Hours, Minutes }
import org.specs2.mock.Mockito
import org.specs2.mutable._

class JobSchedulerIntegrationTest extends SpecificationWithJUnit with Mockito {

  import MockJobUtils._

  "JobScheduler" should {
    "A job creates a failed task and then a successful task from a synchronous job" in {
      val epsilon = Hours.hours(2).toPeriod
      val job1 = new ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD", epsilon)

      val jobGraph = new JobGraph
      val persistenceStore = mock[PersistenceStore]
      val mockTaskManager = mock[TaskManager]

      val scheduler = mockScheduler(epsilon, mockTaskManager, jobGraph, persistenceStore)
      val startTime = DateTime.parse("2012-01-01T01:00:00.000Z")
      scheduler.leader.set(true)
      scheduler.registerJob(job1, persist = true, startTime)

      val newStreams = scheduler.iteration(startTime, scheduler.streams)
      newStreams.head.schedule must_== "R4/2012-01-02T00:00:00.000Z/P1D"

      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, startTime, 0))
      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, startTime, 0))

      there was one(persistenceStore)
        .persistJob(new ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD", epsilon))
      there was one(persistenceStore)
        .persistJob(new ScheduleBasedJob("R4/2012-01-02T00:00:00.000Z/P1D", "job1", "CMD", epsilon))
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
      val mockJobsObserver = mockFullObserver

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore, mockJobsObserver)
      scheduler.leader.set(true)
      scheduler.registerJob(job1, persist = true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.run(() => {
        DateTime.parse("2012-01-01T00:05:01.000Z")
      })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
      val job2 = graph.lookupVertex(jobName).get
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))
      val job3 = graph.lookupVertex(jobName).get
      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0))

      graph.lookupVertex(jobName).get.successCount must_== 2
      graph.lookupVertex(jobName).get.errorCount must_== 1

      there was one(mockJobsObserver).apply(JobFinished(job1, TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0))
      there was one(mockJobsObserver).apply(JobFinished(job2, TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0))
      there was one(mockJobsObserver).apply(JobFailed(Right(job3), TaskUtils.getTaskStatus(job1, DateTime.parse("2012-01-03T00:00:01.000Z"), 0), 0))
    }

    "Tests that a disabled job does not run and does not execute dependant children." in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = true)
      val job2 = new DependencyBasedJob(Set("job1"), name = "job2", command = "CMD", disabled = true)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      scheduler.registerJob(job1, persist = true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.registerJob(job2, persist = true, DateTime.parse("2011-01-01T00:05:01.000Z"))
      scheduler.run(() => {
        DateTime.parse("2012-01-01T00:05:01.000Z")
      })
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
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled = false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled = false)
      val job5 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      val date = DateTime.parse("2011-01-01T00:05:01.000Z")
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.registerJob(job4, persist = true, date)
      scheduler.registerJob(job5, persist = true, date)
      scheduler.run(() => {
        date
      })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0
    }

    "Tests that dependent jobs run even if their parents fail but have softError enabled" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false, retries = 0, softError = true)

      val job3 = new DependencyBasedJob(Set("job1", "job2"), name = "job3", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      val date = DateTime.now(DateTimeZone.UTC)
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.run(() => {
        date
      })

      val finishedDate = date.plusMinutes(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job2, date, 0))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      val vJob2 = graph.lookupVertex("job2").get
      vJob2.successCount must_== 0
      vJob2.errorCount must_== 1

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, DateTime.parse(vJob2.lastError), 0), highPriority = false)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, date, 0), Some(DateTime.parse(vJob2.lastError)))
      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0
    }

    "Tests that dependent jobs don't run if their parents fail without softError enabled" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false, retries = 0, softError = false)

      val job3 = new DependencyBasedJob(Set("job1", "job2"), name = "job3", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      val date = DateTime.now(DateTimeZone.UTC)
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.run(() => {
        date
      })

      val finishedDate = date.plusMinutes(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFailedTask(TaskUtils.getTaskStatus(job2, date, 0))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      val vJob2 = graph.lookupVertex("job2").get
      vJob2.successCount must_== 0
      vJob2.errorCount must_== 1

      there was no(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, DateTime.parse(vJob2.lastError), 0), highPriority = false)
    }

    "Tests that dependent jobs runs when they should after changing the jobgraph" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:01:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled = false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled = false)
      val job5_1 = new DependencyBasedJob(Set("job1", "job2"), name = "job5", command = "CMD", disabled = false)
      val job5_2 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      val date = DateTime.parse("2012-01-01T00:00:00.000Z")
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.registerJob(job4, persist = true, date)
      scheduler.registerJob(job5_1, persist = true, date)

      scheduler.run(() => {
        date
      })

      val finishedDate = date.plusMinutes(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_1, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_1, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      val jobResource = new DependentJobResource(jobScheduler = scheduler, jobGraph = graph)
      jobResource.handleRequest(job5_2)

      scheduler.run(() => {
        date
      })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 2
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 2
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 2
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 2
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_2, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_2, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0
    }

    "Tests that complex dependent jobs run when they should" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val job1 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = "R/2012-01-01T00:00:00.000Z/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled = false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled = false)
      val job5 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled = false)
      val job6 = new DependencyBasedJob(Set("job4", "job5", "job1"), name = "job6", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      val date = DateTime.parse("2011-01-01T00:05:01.000Z")
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.registerJob(job4, persist = true, date)
      scheduler.registerJob(job5, persist = true, date)
      scheduler.registerJob(job6, persist = true, date)
      scheduler.run(() => {
        date
      })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      scheduler.run(() => {
        date
      })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 2
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 2
      graph.lookupVertex("job2").get.errorCount must_== 0

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job6, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job6, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job6").get.successCount must_== 1
      graph.lookupVertex("job6").get.errorCount must_== 0
    }

    "Tests that dependent jobs run when parents are updated" in {
      val epsilon = Minutes.minutes(20).toPeriod
      val date = DateTime.now(DateTimeZone.UTC)
      val fmt = ISODateTimeFormat.dateTime()
      val job1 = new ScheduleBasedJob(schedule = s"R/${fmt.print(date)}/PT1M",
        name = "job1", command = "fooo", epsilon = epsilon, disabled = false)
      val job2 = new ScheduleBasedJob(schedule = s"R/${fmt.print(date)}/PT1M",
        name = "job2", command = "fooo", epsilon = epsilon, disabled = false)

      val job3 = new DependencyBasedJob(Set("job1"), name = "job3", command = "CMD", disabled = false)
      val job4 = new DependencyBasedJob(Set("job1", "job2"), name = "job4", command = "CMD", disabled = false)
      val job5_1 = new DependencyBasedJob(Set("job1", "job2"), name = "job5", command = "CMD", disabled = false)
      val job5_2 = new DependencyBasedJob(Set("job1", "job2", "job3"), name = "job5", command = "CMD", disabled = false)

      val horizon = Minutes.minutes(5).toPeriod
      val mockTaskManager = mock[TaskManager]
      val graph = new JobGraph()
      val mockPersistenceStore = mock[PersistenceStore]

      val scheduler = mockScheduler(horizon, mockTaskManager, graph, mockPersistenceStore)
      scheduler.leader.set(true)
      scheduler.registerJob(job1, persist = true, date)
      scheduler.registerJob(job2, persist = true, date)
      scheduler.registerJob(job3, persist = true, date)
      scheduler.registerJob(job4, persist = true, date)
      scheduler.registerJob(job5_1, persist = true, date)

      scheduler.run(() => {
        date
      })

      val finishedDate = date.plus(1)

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 1
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 1
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was one(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_1, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_1, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 1
      graph.lookupVertex("job5").get.errorCount must_== 0

      val jobResource = new Iso8601JobResource(jobScheduler = scheduler, jobGraph = graph)
      jobResource.handleRequest(job1)
      jobResource.handleRequest(job2)

      scheduler.run(() => {
        date
      })

      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job1, date, 0), Some(finishedDate))
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job2, date, 0), Some(finishedDate))

      graph.lookupVertex("job1").get.successCount must_== 1
      graph.lookupVertex("job1").get.errorCount must_== 0

      graph.lookupVertex("job2").get.successCount must_== 1
      graph.lookupVertex("job2").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job3, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job3, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job3").get.successCount must_== 2
      graph.lookupVertex("job3").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job4, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job4, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job4").get.successCount must_== 2
      graph.lookupVertex("job4").get.errorCount must_== 0

      there was two(mockTaskManager).enqueue(TaskUtils.getTaskId(job5_2, finishedDate, 0), highPriority = false)
      scheduler.handleFinishedTask(TaskUtils.getTaskStatus(job5_2, finishedDate, 0), Some(finishedDate))

      graph.lookupVertex("job5").get.successCount must_== 2
      graph.lookupVertex("job5").get.errorCount must_== 0
    }
  }
}
