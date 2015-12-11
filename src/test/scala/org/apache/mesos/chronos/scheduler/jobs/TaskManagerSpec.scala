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

import com.codahale.metrics.MetricRegistry
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.apache.mesos.chronos.ChronosTestHelper._
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.MesosOfferReviver
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class TaskManagerSpec extends SpecificationWithJUnit with Mockito {
  "TaskManager" should {
    "Calculate the correct time delay between scheduling and dispatching the job" in {
      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mock[PersistenceStore],
        mock[JobGraph], null, MockJobUtils.mockFullObserver, mock[MetricRegistry], makeConfig(),
        mock[MesosOfferReviver])
      val millis = taskManager.getMillisUntilExecution(new DateTime(DateTimeZone.UTC).plus(Hours.ONE))
      val expectedSeconds = scala.math.round(Period.hours(1).toStandardDuration.getMillis / 1000d)
      //Due to startup time / JVM overhead, millis wouldn't be totally accurate.
      val actualSeconds = scala.math.round(millis / 1000d)
      actualSeconds must_== expectedSeconds
    }

    "Handle None job option in getTask" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]

      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mockPersistencStore,
        mockJobGraph, null, MockJobUtils.mockFullObserver, mock[MetricRegistry], makeConfig(),
        mock[MesosOfferReviver])

      val job = new ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M", "test", "sample-command")

      mockJobGraph.lookupVertex("test").returns(Some(job)) // so we can enqueue a job.
      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)

      mockJobGraph.getJobForName("test").returns(None)

      taskManager.getTask must_== None

      there was one(mockPersistencStore).removeTask("ct:1420843781398:0:test:")
    }

    "Revive offers when adding a new task and --revive_offers_for_new_jobs is set" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig("--revive_offers_for_new_jobs")

      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mockPersistencStore,
        mockJobGraph, null, MockJobUtils.mockFullObserver, mock[MetricRegistry], config, mockMesosOfferReviver)

      val job = new ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M", "test", "sample-command")
      mockJobGraph.lookupVertex("test").returns(Some(job)) // so we can enqueue a job.

      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)

      there was one(mockMesosOfferReviver).reviveOffers
    }

    "Don't revive offers when adding a new task and --revive_offers_for_new_jobs is not set" in {
      val mockJobGraph = mock[JobGraph]
      val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val config = makeConfig()

      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mockPersistencStore,
        mockJobGraph, null, MockJobUtils.mockFullObserver, mock[MetricRegistry], config, mockMesosOfferReviver)

      val job = new ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M", "test", "sample-command")
      mockJobGraph.lookupVertex("test").returns(Some(job)) // so we can enqueue a job.

      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)

      there were noCallsTo(mockMesosOfferReviver)
    }
  }
}
