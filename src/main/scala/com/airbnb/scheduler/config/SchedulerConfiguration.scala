package com.airbnb.scheduler.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.bazaarvoice.dropwizard.assets.{AssetsConfiguration, AssetsBundleConfiguration}
import com.yammer.dropwizard.config.Configuration
import org.hibernate.validator.constraints.NotEmpty
import org.rogach.scallop.ScallopConf
import java.net.InetSocketAddress
import mesosphere.marathon.Executor


/**
 * Configuration values that may be parsed from a YAML file.
 * @author Florian Leibert (flo@leibert.de)
 */
class SchedulerConfiguration extends ScallopConf {


    lazy val master = opt[String]("master",
      descr = "The URL of the Mesos master",
      default = Some("master"),
      required = true,
      noshort = true)

//    lazy val mesosFailoverTimeout = opt[Long]("failover_timeout",
//      descr = "The failover_timeout for mesos in seconds (default: 1 week)",
//      default = Some(604800L))
//
//    lazy val highlyAvailable = opt[Boolean]("ha",
//      descr = "Runs Marathon in HA mode with leader election. " +
//        "Allows starting an arbitrary number of other Marathons but all need " +
//        "to be started in HA mode. This mode requires a running ZooKeeper",
//      noshort = true, default = Some(true))
//
//    lazy val checkpoint = opt[Boolean]("checkpoint",
//      descr = "Enable checkpointing of tasks. " +
//        "Requires checkpointing enabled on slaves. Allows tasks to continue " +
//        "running during mesos-slave restarts and upgrades",
//      noshort = true)
//
//    lazy val zooKeeperHostString = opt[String]("zk_hosts",
//      descr = "The list of ZooKeeper servers for storing state",
//      default = Some("localhost:2181"))
//
//    lazy val zooKeeperTimeout = opt[Long]("zk_timeout",
//      descr = "The timeout for ZooKeeper in milliseconds",
//      default = Some(10000L))
//
//    lazy val zooKeeperPath = opt[String]("zk_state",
//      descr = "Path in ZooKeeper for storing state",
//      default = Some("/marathon"))
//
//    lazy val localPortMin = opt[Int]("local_port_min",
//      descr = "Min port number to use when assigning ports to apps",
//      default = Some(10000))
//
//    lazy val localPortMax = opt[Int]("local_port_max",
//      descr = "Max port number to use when assigning ports to apps",
//      default = Some(20000))
//
//    lazy val defaultExecutor = opt[String]("executor",
//      descr = "Executor to use when none is specified",
//      default = Some("//cmd"))
//
//    def zooKeeperStatePath = "%s/state".format(zooKeeperPath())
//
//    def zooKeeperLeaderPath = "%s/leader".format(zooKeeperPath())
//
//    def zooKeeperServerSetPath = "%s/apps".format(zooKeeperPath())
//
//    lazy val hostname = opt[String]("hostname",
//      descr = "The advertised hostname stored in ZooKeeper so another standby " +
//        "host can redirect to this elected leader",
//      default = Some("localhost"))
//
//    def zooKeeperHostAddresses: Seq[InetSocketAddress] =
//      for (s <- zooKeeperHostString().split(",")) yield {
//        val splits = s.split(":")
//        require(splits.length == 2, "expected host:port for zk servers")
//        new InetSocketAddress(splits(0), splits(1).toInt)
//      }
//
//    def executor: Executor = Executor.dispatch(defaultExecutor())
//
//    lazy val mesosRole = opt[String]("mesos_role",
//      descr = "Mesos role for this framework",
//      default = None)


  lazy val  staticAssets = opt[Boolean]("static_assets",
    descr = "Use static assets",
    default = Some(true))


  lazy val  user = opt[String]("user",
    descr = "The mesos user to run the processes under",
    default = Some("root"))

  lazy val  failoverTimeoutSeconds = opt[Int]("failover_timeout",
    descr = "The failover timeout in seconds for Mesos",
    default = Some(1200))


  @JsonProperty
  val scheduleHorizonSeconds =   opt[Int]("schedule_horizon",
    descr = "The look-ahead time for scheduling tasks in seconds",
    default = Some(60))

  @JsonProperty
  val zookeeperServers: String = "localhost:2181"

  @JsonProperty
  val hostname: String = "localhost"

  @JsonProperty
  val executor: String = "shell"

  /**
   * This is the maximum idle time in which a newly elected master doesn't schedule jobs yet.
   */
  @JsonProperty
  val leaderMaxIdleTimeMs: Int = 5000

  @JsonProperty
  val zookeeperTimeoutMs: Int = 5000

  @JsonProperty
  val zookeeperStateZnode: String = "/airbnb/service/chronos/state"

  @JsonProperty
  val zookeeperLeaderZnode: String = "/airbnb/service/chronos/leader"

  @JsonProperty
  val zookeeperCandidateZnode: String = "/airbnb/service/chronos/candidate"

  @JsonProperty
  val defaultJobOwner: String = "flo@airbnb.com"

  @JsonProperty
  val mailServer: Option[String] = None

  @JsonProperty
  val mailUser: Option[String] = None

  @JsonProperty
  val mailPassword: Option[String] = None

  @JsonProperty
  val mailFrom: Option[String] = None

  @JsonProperty
  val mailSslOn: Boolean = false

  @JsonProperty
  val assets: Option[AssetsConfiguration] = None

  @JsonProperty
  val failureRetryDelay: Long = 60000

  @JsonProperty
  val disableAfterFailures: Long = 0

  @JsonProperty
  val mesosTaskMem: Int = 1024

  @JsonProperty
  val mesosTaskCpu: Double = 1

  @JsonProperty
  val mesosTaskDisk: Int = 1024

  @JsonProperty
  val mesosCheckpoint: Boolean = false

  @JsonProperty
  val mesosRole: String = "*"

  @JsonProperty
  val mesosFrameworkName: String = "chronos-1.0.1"

  @JsonProperty
  val gangliaHostPort: Option[String] = None

  @JsonProperty
  val gangliaReportIntervalSeconds: Long = 60L

  @JsonProperty
  val gangliaGroupPrefix: String = ""

  def getAssetsConfiguration: AssetsConfiguration = assets match {
    case Some(assetsConf: AssetsConfiguration) => assetsConf
    case None => new AssetsConfiguration
  }
}
