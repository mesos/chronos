package com.airbnb.scheduler.config

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import scala.collection.mutable.ListBuffer

import com.airbnb.scheduler.state.{PersistenceStore, MesosStatePersistenceStore}
import com.google.inject._
import com.twitter.common.base.Supplier
import com.twitter.common.quantity.Amount
import com.twitter.common.quantity.Time
import com.twitter.common.zookeeper._
import org.apache.mesos.state.ZooKeeperState
import org.apache.zookeeper.ZooDefs

/**
 * Guice glue-code for zookeeper related things.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Consider using Sindi or Subcat for DI.
class ZookeeperModule(val config: SchedulerConfiguration) extends AbstractModule {
  private val log = Logger.getLogger(getClass.getName)

  def configure() { }

  @Inject
  @Singleton
  @Provides
  def provideZookeeperClient(): ZooKeeperClient = {
    val zkServers = new ListBuffer[InetSocketAddress]
    config.zookeeperServers.split(",").map({
      x =>
        require(x.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
        zkServers += new InetSocketAddress(x.split(":")(0), x.split(":")(1).toInt)
    })
    import collection.JavaConversions._
    new ZooKeeperClient(Amount.of(config.zookeeperTimeoutMs, Time.MILLISECONDS), zkServers)
  }

  @Inject
  @Singleton
  @Provides
  def providePersistencStore(zk: ZooKeeperClient): PersistenceStore = {
    log.info("Providing MesosStatePersistenceStore")
    ZooKeeperUtils.ensurePath(zk, ZooDefs.Ids.OPEN_ACL_UNSAFE, config.zookeeperStateZnode)
    new MesosStatePersistenceStore(zk, config, new ZooKeeperState(
      config.zookeeperServers, config.zookeeperTimeoutMs, TimeUnit.MILLISECONDS, config.zookeeperStateZnode))
  }

  @Inject
  @Singleton
  @Provides
  def provideCandidate(zk: ZooKeeperClient): Candidate = {
    log.info("Using hostname:" + config.hostname)
    return new CandidateImpl(new Group(zk, ZooDefs.Ids.OPEN_ACL_UNSAFE, config.zookeeperCandidateZnode),
      CandidateImpl.MOST_RECENT_JUDGE, new Supplier[Array[Byte]] {
        def get() = {
          "%s:%d".format(config.hostname, config.getHttpConfiguration.getPort).getBytes
        }
      })
  }
}
