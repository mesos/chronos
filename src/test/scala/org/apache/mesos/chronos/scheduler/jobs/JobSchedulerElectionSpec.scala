package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.TimeUnit

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.MesosDriverFactory
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.{InstanceSpec, TestingCluster}
import org.apache.curator.utils.{CloseableUtils, EnsurePath}
import org.joda.time.Period
import org.junit.Assert.{assertFalse, assertTrue}
import org.junit.Test
import org.mockito.Mockito.doNothing
import org.specs2.mock.Mockito

class JobSchedulerElectionSpec
  extends Mockito {
  var port = 8080

  @Test
  def testJobSchedulerShouldElectOnlyOneLeader() {
    val testCluster = new TestingCluster(3)
    testCluster.start()

    val (scheduler1: JobScheduler, curator1: CuratorFramework, latch1: LeaderLatch) = scheduler(testCluster.getConnectString)
    val (scheduler2: JobScheduler, curator2: CuratorFramework, latch2: LeaderLatch) = scheduler(testCluster.getConnectString)

    startAndWaitForElection(List(curator1, curator2), List(scheduler1, scheduler2), List(latch1, latch2))

    assertTrue("One candidate, but not both, must be elected", latch1.hasLeadership ^ latch2.hasLeadership)

    assertTrue("One scheduler, but not both, should be leader", scheduler1.isLeader ^ scheduler2.isLeader)
    assertTrue("One scheduler, but not both, should be running", scheduler1.running.get() ^ scheduler2.running.get())

    CloseableUtils.closeQuietly(latch1)
    CloseableUtils.closeQuietly(curator1)
    CloseableUtils.closeQuietly(latch2)
    CloseableUtils.closeQuietly(curator2)
    CloseableUtils.closeQuietly(testCluster)
  }

  @Test
  def testElectLeaderOnZkFailure() {
    val testCluster = new TestingCluster(3)
    testCluster.start()

    val (scheduler1: JobScheduler, curator1: CuratorFramework, latch1: LeaderLatch) = scheduler(testCluster.getConnectString)
    val (scheduler2: JobScheduler, curator2: CuratorFramework, latch2: LeaderLatch) = scheduler(testCluster.getConnectString)

    startAndWaitForElection(List(curator1, curator2), List(scheduler1, scheduler2), List(latch1, latch2))

    val (firstLeader, _) = if (latch1.hasLeadership) (scheduler1, scheduler2)
    else (scheduler2, scheduler1)

    val instance: InstanceSpec = testCluster.findConnectionInstance(firstLeader.curator.getZookeeperClient.getZooKeeper)
    testCluster.killServer(instance)

    awaitElection(List(latch1, latch2))
    assertTrue("After ZK node failure, one candidate, but not both, must be elected", latch1.hasLeadership ^ latch2.hasLeadership)

    CloseableUtils.closeQuietly(latch1)
    CloseableUtils.closeQuietly(curator1)
    CloseableUtils.closeQuietly(latch2)
    CloseableUtils.closeQuietly(curator2)
    CloseableUtils.closeQuietly(testCluster)
  }

  @Test
  def testElectNewLeaderOnMasterFailure() {
    val testCluster = new TestingCluster(3)
    testCluster.start()

    val (scheduler1: JobScheduler, curator1: CuratorFramework, latch1: LeaderLatch) = scheduler(testCluster.getConnectString)
    val (scheduler2: JobScheduler, curator2: CuratorFramework, latch2: LeaderLatch) = scheduler(testCluster.getConnectString)

    startAndWaitForElection(List(curator1, curator2), List(scheduler1, scheduler2), List(latch1, latch2))

    val (leader, follower) = if (latch1.hasLeadership) (scheduler1, scheduler2)
    else (scheduler2, scheduler1)

    leader.shutDown()
    awaitElection(List(latch1, latch2))

    assertTrue("Reserve scheduler's latch should become leader on master failure", follower.leaderLatch.hasLeadership)

    assertTrue("Reserve scheduler should become leader on master failure", follower.isLeader)
    assertFalse("Former master scheduler should not be leader after failure", leader.isLeader)

    assertTrue("Reserve scheduler should start running after master failure", follower.running.get())
    assertFalse("Former master scheduler should not be running after failure", leader.running.get())

    CloseableUtils.closeQuietly(curator1)
    CloseableUtils.closeQuietly(latch2)
    CloseableUtils.closeQuietly(curator2)
    CloseableUtils.closeQuietly(testCluster)
  }

  def scheduler(connectString: String): (JobScheduler, CuratorFramework, LeaderLatch) = {
    val curator = CuratorFrameworkFactory.builder()
      .connectionTimeoutMs(10000)
      .canBeReadOnly(true)
      .connectString(connectString)
      .retryPolicy(new ExponentialBackoffRetry(1000, 20))
      .build()

    curator.start()
    curator.blockUntilConnected(10000, TimeUnit.MILLISECONDS)
    curator.getZookeeperClient.blockUntilConnectedOrTimedOut()
    assertTrue(curator.getZookeeperClient.isConnected)

    val leaderPath = "/chronos/state/candidate"
    val leaderLatch = new LeaderLatch(curator, leaderPath, s"127.0.0.1:$port")
    val persistenceStore = mock[PersistenceStore]
    val mesosDriver: MesosDriverFactory = mock[MesosDriverFactory]

    persistenceStore.getTasks returns Map[String, Array[Byte]]()
    persistenceStore.getJobs returns Iterator[BaseJob]()

    doNothing().when(mesosDriver).start()

    port += 1

    val scheduler = new JobScheduler(
      scheduleHorizon = Period.hours(1),
      taskManager = mock[TaskManager],
      jobGraph = mock[JobGraph],
      persistenceStore = persistenceStore,
      mesosDriver = mesosDriver,
      curator = curator,
      leaderLatch = leaderLatch,
      leaderPath = leaderPath,
      jobMetrics = mock[JobMetrics],
      jobsObserver = mock[JobsObserver]
    )

    val ensurePath: EnsurePath = new EnsurePath(leaderPath)
    ensurePath.ensure(curator.getZookeeperClient)

    (scheduler, curator, leaderLatch)
  }

  def startAndWaitForElection(curators: List[CuratorFramework], schedulers: List[JobScheduler], latches: List[LeaderLatch]) {
    curators.foreach(c => c.getZookeeperClient.blockUntilConnectedOrTimedOut())

    schedulers.foreach({ s =>
      s.startUp()
      Thread.sleep(100)
    })
    awaitElection(latches)

    var maxWaits = 100
    while (!schedulers.exists(_.isLeader) && (maxWaits > 0)) {
      maxWaits -= 1
      Thread.sleep(10)
    }
  }

  def awaitElection(latches: List[LeaderLatch]) {
    var maxWaits = 100
    while (!latches.exists(_.hasLeadership) && (maxWaits > 0)) {
      maxWaits -= 1
      Thread.sleep(10)
    }
    println(s"Waited ${100 - maxWaits} for election")
  }
}
