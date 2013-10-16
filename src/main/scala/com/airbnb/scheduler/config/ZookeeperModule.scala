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
import org.apache.mesos.state.{State, ZooKeeperState}
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.server.{NIOServerCnxn, ZooKeeperServer}
import java.io.File
import mesosphere.mesos.util.FrameworkIdUtil

/**
 * Guice glue-code for zookeeper related things.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Consider using Sindi or Subcut for DI.
class ZookeeperModule(val config: SchedulerConfiguration) extends AbstractModule {
  private val log = Logger.getLogger(getClass.getName)

  def configure() { }

  @Inject
  @Singleton
  @Provides
  def provideZookeeperClient(zkServer: Option[NIOServerCnxn.Factory]): ZooKeeperClient = {
    import collection.JavaConversions._
    new ZooKeeperClient(
      Amount.of(config.zookeeperTimeoutMs(), Time.MILLISECONDS),
      parseZkServers())
  }

  @Inject
  @Singleton
  @Provides
  def provideState(): State = {
    new ZooKeeperState(getZkServerString,
                       config.zookeeperTimeoutMs(),
                       TimeUnit.MILLISECONDS,
                       config.zookeeperStateZnode())
  }

  @Inject
  @Singleton
  @Provides
  def provideStore(zk: ZooKeeperClient, state: State): PersistenceStore = {
    ZooKeeperUtils.ensurePath(zk,
                              ZooDefs.Ids.OPEN_ACL_UNSAFE,
                              config.zookeeperStateZnode())

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
  def provideCandidate(zk: ZooKeeperClient): Candidate = {
    log.info("Using hostname:" + config.hostname())
    return new CandidateImpl(new Group(zk, ZooDefs.Ids.OPEN_ACL_UNSAFE,
      config.zookeeperCandidateZnode()),
      new Supplier[Array[Byte]] {
        def get() = {
          "%s:%d".format(config.hostname, 4400).getBytes  // TODO(tk) get
          // port from http config
        }
      })
  }

  //This is only provided for local mode operation when no zk is given
  //TODO(FL): Cleanup.
  @Inject
  @Singleton
  @Provides
  def provideZookeeperServer(): Option[NIOServerCnxn.Factory] = {
    if (isInProcess) {
      log.warning("Using in-process zookeeper!")
      val tickTime = 10000
      val dataDirectory = System.getProperty("java.io.tmpdir")
      val tmpTime = System.currentTimeMillis()
      val dir = new File(dataDirectory, "chronos-zookeeper-%d".format(tmpTime)).getAbsoluteFile
      if (!dir.exists()) {
        require(dir.mkdir(), "Cannot create directory for internal zookeeper:" + dir.toString)
      } else {
        require(dir.isDirectory, "File %s is not a directory!".format(dir.toString))
      }
      val server = new ZooKeeperServer(dir, dir, tickTime)
      val zkServers = parseZkServers()
      val standaloneServerFactory = new NIOServerCnxn.Factory(
        new InetSocketAddress(zkServers(0).getPort))
      standaloneServerFactory.startup(server)
      Some(standaloneServerFactory)
    } else {
      log.info("Using external zookeeper.")
      None
    }
  }

  private def isInProcess: Boolean = {
    log.info(config.zookeeperServers)
    val servers = config.zookeeperServers().split(",")
    return servers.size == 1 && servers(0).split(":")(0) == "--"
  }

  private def getZkServerString: String = {
    if (isInProcess) {
      config.zookeeperServers().replaceAll("--", "localhost")
    } else {
      config.zookeeperServers()
    }
  }

  private def parseZkServers(): List[InetSocketAddress] = {
    val servers: String = config.zookeeperServers().split(",")
    if (isInProcess) {
      //TODO(FL): Refactor this code.
      List(new InetSocketAddress("localhost", servers(0).split(":")(1).toInt))
    } else {
    servers.map({
      server =>
        require(server.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
        if (server.startsWith("--")) {
          log.warning("Replacing '--' with 'localhost', using in-process zookeeper.")
          new InetSocketAddress("localhost", server.split(":")(1).toInt)
        } else {
          new InetSocketAddress(server.split(":")(0), server.split(":")(1).toInt)
        }
    }).toList
    }
  }
}
