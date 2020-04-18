package org.apache.mesos.chronos.scheduler.config

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.jobs.ZookeeperService
import org.apache.mesos.chronos.scheduler.state.{MesosStatePersistenceStore, PersistenceStore}
import com.google.inject.{AbstractModule, Inject, Provides, Singleton}
import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.mesos.state.{State, ZooKeeperState}

/**
 * Guice glue-code for zookeeper related things.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Consider using Sindi or Subcut for DI.
class ZookeeperModule(val config: SchedulerConfiguration with HttpConf)
  extends AbstractModule {
  private val log = Logger.getLogger(getClass.getName)

  def configure() {}

  @Inject
  @Singleton
  @Provides
  def provideZookeeperClient(): CuratorFramework = {
    val curator = CuratorFrameworkFactory.builder()
      .connectionTimeoutMs(config.zooKeeperTimeout().toInt)
      .canBeReadOnly(false)
      .connectString(validateZkServers())
      .retryPolicy(new ExponentialBackoffRetry(1000, 10))
      .build()

    curator.start()
    log.info("Connecting to ZK...")
    curator.blockUntilConnected()
    curator
  }

  private def validateZkServers(): String = {
    val servers: Array[String] = config.zookeeperServers().split(",")
    servers.foreach({
      server =>
        require(server.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
    })
    servers.mkString(",")
  }

  @Inject
  @Singleton
  @Provides
  def provideZookeeperService(curator: CuratorFramework): ZookeeperService = {
    new ZookeeperService(curator)
  }

  @Inject
  @Singleton
  @Provides
  def provideState(): State = {
    new ZooKeeperState(config.zookeeperServers(),
      config.zooKeeperTimeout(),
      TimeUnit.MILLISECONDS,
      config.zooKeeperStatePath)
  }

  @Inject
  @Singleton
  @Provides
  def provideStore(zk: CuratorFramework, state: State): PersistenceStore = {
    if (zk.checkExists().forPath(config.zooKeeperStatePath) != null) {
      zk.checkExists().creatingParentContainersIfNeeded()
    } else {
      zk.create().creatingParentContainersIfNeeded()
    }

    new MesosStatePersistenceStore(zk, config, state)
  }

  @Provides
  @Singleton
  def provideFrameworkIdUtil(state: State): FrameworkIdUtil = {
    new FrameworkIdUtil(state)
  }

  @Inject
  @Singleton
  @Provides
  def provideLeaderLatch(curator: CuratorFramework): LeaderLatch = {
    if (curator.checkExists().forPath(config.zooKeeperCandidatePath) != null) {
      curator.checkExists().creatingParentContainersIfNeeded()
    } else {
      curator.create().creatingParentContainersIfNeeded()
    }

    val id = "%s:%d".format(config.hostname(), config.httpPort())
    new LeaderLatch(curator, config.zooKeeperCandidatePath, id)
  }
}
