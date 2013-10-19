package com.airbnb.scheduler.config

import org.rogach.scallop.ScallopConf
import java.net.InetSocketAddress


/**
 * Configuration values that may be parsed from a YAML file.
 * @author Florian Leibert (flo@leibert.de)
 */
trait SchedulerConfiguration extends ScallopConf {

  lazy val master = opt[String]("master",
    descr = "The URL of the Mesos master",
    default = Some("zk://localhost:2181/mesos"),
    required = true,
    noshort = true)

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

  lazy val zookeeperServers = opt[String]("zk_hosts",
    descr = "The list of ZooKeeper servers for storing state",
    default = Some("localhost:2181"))

  def zooKeeperHostAddresses: Seq[InetSocketAddress] =
    for (s <- zookeeperServers().split(",")) yield {
      val splits = s.split(":")
      require(splits.length == 2, "expected host:port for zk servers")
      new InetSocketAddress(splits(0), splits(1).toInt)
    }

  lazy val hostname = opt[String]("hostname",
    descr = "The advertised hostname stored in ZooKeeper so another standby " +
      "host can redirect to this elected leader",
    default = Some("localhost"))

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

  lazy val disableAfterFailures = opt[Long]("disable_after_failures",
    descr = "Disables a job after this many failures have occurred",
    default = Some(0))

  lazy val mesosTaskMem = opt[Int]("mesos_task_mem",
    descr = "Amount of memory to request from Mesos for each task (MB)",
    default = Some(128))

  lazy val mesosTaskCpu = opt[Double]("mesos_task_cpu",
    descr = "Number of CPUs to request from Mesos for each task",
    default = Some(0.1))

  lazy val mesosTaskDisk = opt[Int]("mesos_task_disk",
    descr = "Amount of disk capacity to request from Mesos for each task (MB)",
    default = Some(256))

  lazy val mesosCheckpoint = opt[Boolean]("mesos_checkpoint",
    descr = "Enable checkpointing in Mesos",
    default = Some(false))

  lazy val mesosRole = opt[String]("mesos_role",
    descr = "The Mesos role to run tasks under",
    default = Some("*"))

  // Chronos version
  lazy val version = "2.0.1"

  lazy val mesosFrameworkName = opt[String]("mesos_framework_name",
    descr = "The framework name",
    default = Some("chronos-" + version))
}
