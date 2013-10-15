package com.airbnb.scheduler.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.rogach.scallop.ScallopConf
import java.net.InetSocketAddress


/**
 * Configuration values that may be parsed from a YAML file.
 * @author Florian Leibert (flo@leibert.de)
 */
trait SchedulerConfiguration extends ScallopConf {


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


  lazy val staticAssets = opt[Boolean]("static_assets",
    descr = "Use static assets",
    default = Some(true))


  lazy val user = opt[String]("user",
    descr = "The mesos user to run the processes under",
    default = Some("root"))

  //TODO(FL): Be consistent and do everything in millis
  lazy val failoverTimeoutSeconds = opt[Int]("failover_timeout",
    descr = "The failover timeout in seconds for Mesos",
    default = Some(1200))

  lazy val scheduleHorizonSeconds = opt[Int]("schedule_horizon",
    descr = "The look-ahead time for scheduling tasks in seconds",
    default = Some(60))

  lazy val zooKeeperHostString = opt[String]("zk_hosts",
    descr = "The list of ZooKeeper servers for storing state",
    default = Some("localhost:2181"))

  def zooKeeperHostAddresses: Seq[InetSocketAddress] =
    for (s <- zooKeeperHostString().split(",")) yield {
      val splits = s.split(":")
      require(splits.length == 2, "expected host:port for zk servers")
      new InetSocketAddress(splits(0), splits(1).toInt)
    }


  lazy val hostname = opt[String]("hostname",
    descr = "The advertised hostname stored in ZooKeeper so another standby " +
      "host can redirect to this elected leader",
    default = Some("localhost"))

  def executor = defaultExecutor

  lazy val defaultExecutor = opt[String]("executor",
    descr = "Executor to use when none is specified",
    default = Some("//cmd"))


  lazy val leaderMaxIdleTimeMs = opt[Int]("leader_max_idle_time",
    descr = "The look-ahead time for scheduling tasks in milliseconds",
    default = Some(5000))

  lazy val leaderMaxIdleTimeMs = opt[Int]("leader_max_idle_time",
    descr = "The look-ahead time for scheduling tasks in milliseconds",
    default = Some(5000))

  lazy val zooKeeperTimeout = opt[Long]("zk_timeout",
    descr = "The timeout for ZooKeeper in milliseconds",
    default = Some(10000L))

  lazy val zooKeeperPath = opt[String]("zk_path",
    descr = "Path in ZooKeeper for storing state",
    default = Some("/chronos/state"))

  def zooKeeperStatePath = "%s/state".format(zooKeeperPath())

  def zooKeeperLeaderPath = "%s/leader".format(zooKeeperPath())

  def zooKeeperCandidatePath = "%s/candidate".format(zooKeeperPath())

  lazy val defaultJobOwner = opt[String]("default_job_owner",
    descr = "Job Owner",
    default = Some("flo@mesosphe.re"))

  lazy val mailServer = opt[String]("mail_server",
    descr = "Address of the mailserver",
    default = None)

  lazy val mailUser = opt[String]("mail_user",
    descr = "Mail user (for auth)",
    default = None)

  lazy val mailPassword = opt[String]("mail_password",
    descr = "Mail password (for auth)",
    default = None)

  lazy val mailFrom = opt[String]("mail_from",
    descr = "Mail from field",
    default = None)

  lazy val mailSslOn = opt[Boolean]("mail_ssl",
    descr = "Mail SSL",
    default = Some(false))

  lazy val failureRetryDelayMs = opt[Long]("failure_retry",
    descr = "Number of ms between retries",
    default = Some(60000))


  lazy val disableAfterFailures = opt[Long]("disable_time_after_failure",
    descr = "Disables a job for the time duration after a failure",
    default = Some(0))


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

}
