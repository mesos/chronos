package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import com.codahale.metrics.MetricRegistry
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._

class TaskManagerSpec extends SpecificationWithJUnit with Mockito {

  "TaskManager" should {
    "Calculate the correct time delay between scheduling and dispatching the job" in {
      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mock[PersistenceStore],
        mock[JobGraph], null, MockJobUtils.mockFullObserver, mock[MetricRegistry])
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
        mockJobGraph, null, mock[JobStats], mock[MetricRegistry])

      val job = new ScheduleBasedJob("R/2012-01-01T00:00:01.000Z/PT1M", "test", "sample-command")

      mockJobGraph.lookupVertex("test").returns(Some(job)) // so we can enqueue a job.
      taskManager.enqueue("ct:1420843781398:0:test:", highPriority = true)

      mockJobGraph.getJobForName("test").returns(None)

      taskManager.getTask must_== None

      there was one(mockPersistencStore).removeTask("ct:1420843781398:0:test:")
    }
  }
}
