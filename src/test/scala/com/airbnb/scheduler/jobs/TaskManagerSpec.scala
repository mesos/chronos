package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.state.PersistenceStore
import com.google.common.util.concurrent.{ListeningScheduledExecutorService}
import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._
import com.airbnb.scheduler.graph.JobGraph
import com.codahale.metrics.MetricRegistry

class TaskManagerSpec extends SpecificationWithJUnit with Mockito {

  "TaskManager" should {
    "Calculate the correct time delay between scheduling and dispatching the job" in {
      val taskManager = new TaskManager(mock[ListeningScheduledExecutorService], mock[PersistenceStore],
        mock[JobGraph], null, mock[MetricRegistry])
      val millis = taskManager.getMillisUntilExecution(new DateTime(DateTimeZone.UTC).plus(Hours.ONE))
      val expectedSeconds = scala.math.round(Period.hours(1).toStandardDuration.getMillis / 1000d)
      //Due to startup time / JVM overhead, millis wouldn't be totally accurate.
      val actualSeconds = scala.math.round(millis / 1000d)
      actualSeconds must_== expectedSeconds
    }
  }
}
