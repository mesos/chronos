package org.apache.mesos.chronos.scheduler.jobs

import com.codahale.metrics.MetricRegistry
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.apache.mesos.Protos.{Filters, OfferID, Status, TaskState}
import org.apache.mesos.SchedulerDriver
import org.apache.mesos.chronos.ChronosTestHelper.makeConfig
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.{MesosDriverFactory, MesosOfferReviver}
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
    mesosDriverFactory.get returns mockSchedulerDriver
  }

  def mockTaskManager: TaskManager = {
    val mockJobGraph = mock[JobGraph]
    val mockPersistencStore: PersistenceStore = mock[PersistenceStore]
    val mockMesosOfferReviver = mock[MesosOfferReviver]
    val config = makeConfig()

    val mockTaskManager = new TaskManager(mock[ListeningScheduledExecutorService], mockPersistencStore,
      mockJobGraph, null, MockJobUtils.mockFullObserver, mock[MetricRegistry], config, mockMesosOfferReviver)
    mockTaskManager
  }
}
