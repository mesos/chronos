package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.state.{MesosStatePersistenceStore, PersistenceStore}
import com.twitter.common.zookeeper.ZooKeeperClient
import org.specs2.mock._
import org.specs2.mutable._
import org.apache.zookeeper.ZooKeeper

class JobUtilsSpec extends SpecificationWithJUnit with Mockito {

  "Save a ScheduleBasedJob job correctly and be able to load it" in {
    val mockZKClient = mock[ZooKeeperClient]
    val mockZK = mock[ZooKeeper]
    val config = new SchedulerConfiguration
    val store = new MesosStatePersistenceStore(mockZKClient, config)
    val startTime = "R1/2012-01-01T00:00:01.000Z/PT1M"
    val job = new ScheduleBasedJob(startTime, "sample-name", "sample-command")
    val mockScheduler = mock[JobScheduler]

    mockZKClient.get returns (mockZK)

    store.persistJob(job)
    JobUtils.loadJobs(mockScheduler, store)

    there was one(mockScheduler).registerJob(List(job), true)
  }

}
