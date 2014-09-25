package com.airbnb.scheduler.config

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.airbnb.scheduler.state.{PersistenceStore, MesosStatePersistenceStore}
import com.google.inject.{AbstractModule, Inject, Provides, Singleton}
import org.apache.mesos.state.{State, ZooKeeperState}
import mesosphere.mesos.util.FrameworkIdUtil
import mesosphere.chaos.http.HttpConf
import org.apache.curator.framework.{CuratorFrameworkFactory, CuratorFramework}
import org.apache.curator.retry.ExponentialBackoffRetry
import com.airbnb.scheduler.jobs.ZookeeperService
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.utils.EnsurePath

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
            .canBeReadOnly(true)
            .connectString(validateZkServers())
            .retryPolicy(new ExponentialBackoffRetry(1000, 10))
            .build()

    curator.start()
    log.info("Connecting to ZK...")
    curator.blockUntilConnected()
    curator
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
    val ensurePath: EnsurePath = new EnsurePath(config.zooKeeperStatePath)
    ensurePath.ensure(zk.getZookeeperClient)

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
    val ensurePath: EnsurePath = new EnsurePath(config.zooKeeperCandidatePath)
    ensurePath.ensure(curator.getZookeeperClient)

    val id = "%s:%d".format(config.hostname(), config.httpPort())
    new LeaderLatch(curator, config.zooKeeperCandidatePath, id)
  }

  private def validateZkServers(): String = {
    val servers: Array[String] = config.zookeeperServers().split(",")
    servers.foreach({
      server =>
        require(server.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
    })
    servers.mkString(",")
  }
}
