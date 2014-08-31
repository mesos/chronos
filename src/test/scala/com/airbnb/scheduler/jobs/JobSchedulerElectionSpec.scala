package com.airbnb.scheduler.jobs

import org.specs2.mock.Mockito
import org.mockito.Mockito.doNothing
import org.apache.curator.test.{TestingZooKeeperServer, InstanceSpec, TestingCluster}
import org.joda.time.Period
import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.PersistenceStore
import org.apache.curator.framework.{CuratorFrameworkFactory, CuratorFramework}
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.retry.ExponentialBackoffRetry

import org.apache.curator.utils.EnsurePath
import com.airbnb.scheduler.mesos.MesosDriverFactory
import org.junit.{After, Before, Test}
import org.junit.Assert.{assertFalse, assertTrue}


class JobSchedulerElectionSpec
        extends Mockito
{
  var port = 8080

  def scheduler(connectString: String): (JobScheduler, CuratorFramework, LeaderLatch) = {
    val curatorBuilder = CuratorFrameworkFactory.builder()
            .connectionTimeoutMs(10)
            .canBeReadOnly(true)
            .connectString(connectString)
            .retryPolicy(new ExponentialBackoffRetry(1000, 100))

    val curator = curatorBuilder.build()
    val leaderPath = "/chronos/state/candidate"
    val leaderLatch = new LeaderLatch(curator, leaderPath, s"127.0.0.1:${port}")
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
      jobStats = mock[JobStats]
    )

    val ensurePath: EnsurePath = new EnsurePath(leaderPath)
    ensurePath.ensure(curator.getZookeeperClient)

    (scheduler, curator, leaderLatch)
  }

  def startAndWaitForElection(curators: List[CuratorFramework], schedulers: List[JobScheduler], latches: List[LeaderLatch]) {
    curators.foreach(c => c.start())
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

  var testCluster: TestingCluster = null

  @Before
  def createAndStartCluster() {
    testCluster = new TestingCluster(3)
    testCluster.start()

    Thread.sleep(5)
  }

  @After
  def stopCluster() {
    testCluster.stop()
    testCluster = null
  }

  @Test
  def testJobSchedulerShouldElectOnlyOneLeader() {
    val (scheduler1: JobScheduler, curator1: CuratorFramework, latch1: LeaderLatch) = scheduler(testCluster.getConnectString)
    val (scheduler2: JobScheduler, curator2: CuratorFramework, latch2: LeaderLatch) = scheduler(testCluster.getConnectString)

    startAndWaitForElection(List(curator1, curator2), List(scheduler1, scheduler2), List(latch1, latch2))

    assertTrue("One candidate, but not both, must be elected", latch1.hasLeadership ^ latch2.hasLeadership)

    assertTrue("One scheduler, but not both, should be leader", scheduler1.isLeader ^ scheduler2.isLeader)
    assertTrue("One scheduler, but not both, should be running", scheduler1.running.get() ^ scheduler2.running.get())
  }

  @Test
  def testElectLeaderOnZkFailure() {
    val (scheduler1: JobScheduler, curator1: CuratorFramework, latch1: LeaderLatch) = scheduler(testCluster.getConnectString)
    val (scheduler2: JobScheduler, curator2: CuratorFramework, latch2: LeaderLatch) = scheduler(testCluster.getConnectString)

    startAndWaitForElection(List(curator1, curator2), List(scheduler1, scheduler2), List(latch1, latch2))

    val (firstLeader, _) = if (latch1.hasLeadership) (scheduler1, scheduler2)
    else (scheduler2, scheduler1)

    val instance: InstanceSpec = testCluster.findConnectionInstance(firstLeader.curator.getZookeeperClient.getZooKeeper)
    testCluster.killServer(instance)

    awaitElection(List(latch1, latch2))
    assertTrue("After ZK node failure, one candidate, but not both, must be elected", latch1.hasLeadership ^ latch2.hasLeadership)
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
  }
}
