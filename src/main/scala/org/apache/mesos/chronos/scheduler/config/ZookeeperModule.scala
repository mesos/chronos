package org.apache.mesos.chronos.scheduler.config

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.google.inject.{AbstractModule, Inject, Provides, Singleton}
import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.mesos.chronos.scheduler.jobs.ZookeeperService
import org.apache.mesos.chronos.scheduler.state.{MesosStatePersistenceStore, PersistenceStore}
import org.apache.mesos.state.{State, ZooKeeperState}
import org.apache.zookeeper.KeeperException

/**
  * Guice glue-code for zookeeper related things.
  *
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
    val builder = CuratorFrameworkFactory.builder()
      .connectionTimeoutMs(config.zooKeeperTimeout().toInt)
      .canBeReadOnly(false)
      .connectString(validateZkServers())
      .retryPolicy(new ExponentialBackoffRetry(1000, 10))

    config.zooKeeperAuth.get match {
      case Some(s) =>
        builder.authorization("digest", s.getBytes())
      case _ =>
    }

    val curator = builder.build()

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
      config.zooKeeperStatePath,
      getAuthScheme,
      getAuthBytes
    )
  }

  private def getAuthScheme: String = {
    config.zooKeeperAuth.get match {
      case Some(_) =>
        "digest"
      case _ => null
    }
  }

  private def getAuthBytes: Array[Byte] = {
    config.zooKeeperAuth.get match {
      case Some(s) => s.getBytes()
      case _ => null
    }
  }

  @Inject
  @Singleton
  @Provides
  def provideStore(curator: CuratorFramework, state: State): PersistenceStore = {
    scala.util.control.Exception.ignoring(classOf[KeeperException.NodeExistsException]) {
      curator.create()
        .creatingParentContainersIfNeeded()
        .forPath(config.zooKeeperStatePath)
    }
    new MesosStatePersistenceStore(curator, config, state)
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
    val id = "%s:%d".format(config.hostname(), config.httpPort())

    scala.util.control.Exception.ignoring(classOf[KeeperException.NodeExistsException]) {
      curator.create()
        .creatingParentContainersIfNeeded()
        .forPath(config.zooKeeperCandidatePath)
    }

    new LeaderLatch(curator, config.zooKeeperCandidatePath, id)
  }
}
