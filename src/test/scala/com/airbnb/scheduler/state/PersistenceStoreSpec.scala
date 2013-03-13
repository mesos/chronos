package com.airbnb.scheduler.state

import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.config.SchedulerConfiguration
import com.google.protobuf.ByteString
import com.twitter.common.zookeeper.ZooKeeperClient
import org.apache.mesos.Protos.{SlaveID, TaskID, TaskInfo}
import org.apache.zookeeper.ZooKeeper
import org.joda.time.Hours
import org.specs2.mutable._
import org.specs2.mock._

class PersistenceStoreSpec extends SpecificationWithJUnit with Mockito
{

  "MesosStatePersistenceStore" should {

    "Writing and reading a job works" in {
      val store = new MesosStatePersistenceStore(null, null)
      val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
      val job = new ScheduleBasedJob(schedule = startTime, name = "sample-name", command = "sample-command",
                                     epsilon = Hours.hours(1).toPeriod, successCount = 1L, executor = "fooexecutor", executorFlags = "args")

      store.persistJob(job)
      val job2 = store.getJob(job.name)

      job2.name must_== job.name
      job2.executor must_== job.executor
      job2.successCount must_== job.successCount
      job2.command must_== job.command

    }
    //TODO(FL): Write mock test for the DependencyBasedJob
  }

}
