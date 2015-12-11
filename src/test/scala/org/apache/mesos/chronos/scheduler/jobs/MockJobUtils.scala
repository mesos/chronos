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

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.Period
import org.specs2.mock._

object MockJobUtils extends Mockito {
  def mockScheduler(horizon: Period,
                    taskManager: TaskManager,
                    jobGraph: JobGraph,
                    persistenceStore: PersistenceStore = mock[PersistenceStore],
                    jobsObserver: JobsObserver.Observer = mock[JobsObserver.Observer]): JobScheduler =
    new JobScheduler(horizon, taskManager, jobGraph, persistenceStore,
      jobMetrics = mock[JobMetrics], jobsObserver = jobsObserver)

  def mockFullObserver: JobsObserver.Observer = {
    val observer = mock[JobsObserver.Observer]
    observer.apply(any[JobEvent]) returns Unit
  }
}
