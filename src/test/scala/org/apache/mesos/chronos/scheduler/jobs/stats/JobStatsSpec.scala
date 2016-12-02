package org.apache.mesos.chronos.scheduler.jobs.stats

import mesosphere.mesos.protos._
import org.apache.mesos.chronos.scheduler.config.CassandraConfiguration
import org.apache.mesos.chronos.scheduler.jobs._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class JobStatsSpec extends SpecificationWithJUnit with Mockito {
  "JobStats" should {
    "Correctly return states" in {
      import mesosphere.mesos.protos.Implicits._
      val jobStats = new JobStats(None, mock[CassandraConfiguration])

      val observer = jobStats.asObserver

      val job1 = ScheduleBasedJob("R5/2012-01-01T00:00:00.000Z/P1D", "job1", "CMD")

      observer.apply(JobQueued(job1, "ct:0:1:job1:", 0))
      jobStats.getJobState("job1") must_== "queued"
      observer.apply(JobStarted(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskRunning), 0, 1))
      jobStats.getJobState("job1") must_== "1 running"
      observer.apply(JobFinished(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskFinished), 0, 0))
      jobStats.getJobState("job1") must_== "idle"

      observer.apply(JobQueued(job1, "ct:0:1:job1:", 0))
      jobStats.getJobState("job1") must_== "queued"
      observer.apply(JobStarted(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskRunning), 0, 1))
      jobStats.getJobState("job1") must_== "1 running"
      observer.apply(JobStarted(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskRunning), 0, 2))
      jobStats.getJobState("job1") must_== "2 running"
      observer.apply(JobStarted(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskRunning), 0, 3))
      jobStats.getJobState("job1") must_== "3 running"
      observer.apply(JobFinished(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskFinished), 0, 2))
      jobStats.getJobState("job1") must_== "2 running"
      observer.apply(JobFinished(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskFinished), 0, 1))
      jobStats.getJobState("job1") must_== "1 running"
      observer.apply(JobQueued(job1, "ct:0:1:job1:", 0))
      jobStats.getJobState("job1") must_== "1 running"
      observer.apply(JobStarted(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskRunning), 0, 2))
      jobStats.getJobState("job1") must_== "2 running"
      observer.apply(JobFailed(Right(job1), TaskStatus(TaskID("ct:0:1:job1:"), TaskFailed), 0, 1))
      jobStats.getJobState("job1") must_== "1 running"
      observer.apply(JobFinished(job1, TaskStatus(TaskID("ct:0:1:job1:"), TaskFinished), 0, 0))
      jobStats.getJobState("job1") must_== "idle"
    }
  }
}
