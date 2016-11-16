package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.Period
import org.specs2.mock._

object MockJobUtils extends Mockito {
  def mockScheduler(taskManager: TaskManager,
                    jobGraph: JobGraph,
                    persistenceStore: PersistenceStore = mock[PersistenceStore],
                    jobsObserver: JobsObserver.Observer = mock[JobsObserver.Observer]): JobScheduler =
    new JobScheduler(taskManager, jobGraph, persistenceStore,
      jobMetrics = mock[JobMetrics], jobsObserver = jobsObserver)

  def mockFullObserver: JobsObserver.Observer = {
    val observer = mock[JobsObserver.Observer]
    observer.apply(any[JobEvent]) returns Unit
  }
}
