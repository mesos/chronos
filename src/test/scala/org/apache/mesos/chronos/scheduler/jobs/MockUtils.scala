package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.Period
import org.specs2.mock._

object MockUtils extends Mockito {
  def mockScheduler(horizon: Period,
                    taskManager: TaskManager,
                    jobGraph: JobGraph,
                    persistenceStore: PersistenceStore = mock[PersistenceStore],
                     jobsObserver: JobsObserver = mock[JobsObserver]): JobScheduler = {
    new JobScheduler(horizon, taskManager, jobGraph, persistenceStore,
      jobMetrics = mock[JobMetrics], jobsObserver = jobsObserver)
  }
}
