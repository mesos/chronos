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
package org.apache.mesos.chronos.scheduler.state

import org.apache.mesos.chronos.scheduler.jobs._
import org.joda.time.Hours
import org.specs2.mock._
import org.specs2.mutable._

class PersistenceStoreSpec extends SpecificationWithJUnit with Mockito {

  "MesosStatePersistenceStore" should {

    "Writing and reading ScheduledBasedJob a job works" in {
      val store = new MesosStatePersistenceStore(null, null)
      val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
      val job = new ScheduleBasedJob(schedule = startTime, name = "sample-name",
        command = "sample-command", successCount = 1L, epsilon = Hours.hours(1).toPeriod,
        executor = "fooexecutor", executorFlags = "args")

      store.persistJob(job)
      val job2 = store.getJob(job.name)

      job2.name must_== job.name
      job2.executor must_== job.executor
      job2.successCount must_== job.successCount
      job2.command must_== job.command

    }

    "Writing and reading DependencyBasedJob a job works" in {
      val store = new MesosStatePersistenceStore(null, null)
      val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
      val epsilon = Hours.hours(1).toPeriod
      val schedJob = new ScheduleBasedJob(schedule = startTime, name = "sample-name",
        command = "sample-command", epsilon = epsilon)
      val job = new DependencyBasedJob(parents = Set("sample-name"),
        name = "sample-dep", command = "sample-command",
        epsilon = epsilon, softError = true,
        successCount = 1L, errorCount = 0L,
        executor = "fooexecutor", executorFlags = "-w",
        retries = 1, disabled = false)

      store.persistJob(job)
      val job2 = store.getJob(job.name)

      job2.name must_== job.name
      job2.command must_== job.command
      job2.softError must_== job.softError
      job2.successCount must_== job.successCount
      job2.errorCount must_== job.errorCount
      job2.executor must_== job.executor
      job2.executorFlags must_== job.executorFlags
      job2.retries must_== job.retries
      job2.disabled must_== job.disabled
    }
  }

}
