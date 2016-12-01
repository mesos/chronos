package org.apache.mesos.chronos.scheduler.jobs

import com.google.common.cache.CacheBuilder
import org.apache.mesos.Protos.{Filters, OfferID, Status, TaskState}
import org.apache.mesos.SchedulerDriver
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.MesosDriverFactory
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
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

  def mockDriverFactory: MesosDriverFactory = {
    val mockSchedulerDriver = mock[SchedulerDriver]
    mockSchedulerDriver.reviveOffers() returns Status.DRIVER_RUNNING
    mockSchedulerDriver.declineOffer(any[OfferID], any[Filters]) returns Status.DRIVER_RUNNING
    val mesosDriverFactory = mock[MesosDriverFactory]
    mesosDriverFactory.get() returns mockSchedulerDriver
  }

  def mockTaskManager: TaskManager = {
    val mockTaskManager = mock[TaskManager]
    val cache = CacheBuilder.newBuilder().maximumSize(10L).build[String, TaskState]()
    mockTaskManager.getTaskCache returns cache
  }
}
