package org.apache.mesos.chronos.scheduler.config

import java.net.InetSocketAddress

import org.rogach.scallop.ScallopConf


/**
 * Configuration values that may be parsed from a YAML file.
 * @author Florian Leibert (flo@leibert.de)
 */
trait SchedulerConfiguration extends ScallopConf {

  lazy val master = opt[String]("master",
    descr = "The URL of the Mesos master",
    default = Some("local"),
    required = true,
    noshort = true)

  lazy val user = opt[String]("user",
    descr = "The chronos user to run the processes under",
    default = Some("root"))

  //TODO(FL): Be consistent and do everything in millis
  lazy val failoverTimeoutSeconds = opt[Int]("failover_timeout",
    descr = "The failover timeout in seconds for Mesos",
    default = Some(604800))

  lazy val scheduleHorizonSeconds = opt[Int]("schedule_horizon",
    descr = "The look-ahead time for scheduling tasks in seconds",
    default = Some(60))

  lazy val clusterName = opt[String]("cluster_name",
    descr = "The name of the cluster where Chronos is run",
    default = None)
  lazy val zookeeperServersString = opt[String]("zk_hosts",
    descr = "The list of ZooKeeper servers for storing state",
    default = Some("localhost:2181"))
  lazy val hostname = opt[String]("hostname",
    descr = "The advertised hostname stored in ZooKeeper so another standby " +
      "host can redirect to this elected leader",
    default = Some(java.net.InetAddress.getLocalHost.getHostName))
  lazy val leaderMaxIdleTimeMs = opt[Int]("leader_max_idle_time",
    descr = "The look-ahead time for scheduling tasks in milliseconds",
    default = Some(5000))
  lazy val zooKeeperTimeout = opt[Long]("zk_timeout",
    descr = "The timeout for ZooKeeper in milliseconds",
    default = Some(10000L))
  lazy val zooKeeperPath = opt[String]("zk_path",
    descr = "Path in ZooKeeper for storing state",
    default = Some("/chronos/state"))
  lazy val mailServer = opt[String]("mail_server",
    descr = "Address of the mailserver in server:port format",
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
  lazy val ravenDsn = opt[String]("raven_dsn",
    descr = "Raven DSN for connecting to a raven or sentry event service",
    default = None)
  lazy val slackWebhookUrl = opt[String]("slack_url",
    descr = "Webhook URL for posting to Slack",
    default = None)
  lazy val slackToken = opt[String]("slack_token",
    descr = "Token needed for posting to Slack",
    default = None)
  lazy val slackChannel = opt[String]("slack_channel",
    descr = "The channel to post to in Slack",
    default = None)
  lazy val failureRetryDelayMs = opt[Long]("failure_retry",
    descr = "Number of ms between retries",
    default = Some(60000))
  lazy val disableAfterFailures = opt[Long]("disable_after_failures",
    descr = "Disables a job after this many failures have occurred",
    default = Some(0))
  lazy val mesosTaskMem = opt[Double]("mesos_task_mem",
    descr = "Amount of memory to request from Mesos for each task (MB)",
    default = Some(128.0))
  lazy val mesosTaskCpu = opt[Double]("mesos_task_cpu",
    descr = "Number of CPUs to request from Mesos for each task",
    default = Some(0.1))
  lazy val mesosTaskDisk = opt[Double]("mesos_task_disk",
    descr = "Amount of disk capacity to request from Mesos for each task (MB)",
    default = Some(256.0))
  lazy val mesosCheckpoint = opt[Boolean]("mesos_checkpoint",
    descr = "Enable checkpointing in Mesos",
    default = Some(true))
  lazy val mesosRole = opt[String]("mesos_role",
    descr = "The Mesos role to run tasks under",
    default = Some("*"))
  lazy val taskEpsilon = opt[Int]("task_epsilon",
    descr = "The default epsilon value for tasks, in seconds",
    default = Some(60))
  // Chronos version
  lazy val version =
    Option(classOf[SchedulerConfiguration].getPackage.getImplementationVersion).getOrElse("unknown")
  lazy val mesosFrameworkName = opt[String]("mesos_framework_name",
    descr = "The framework name",
    default = Some("chronos-" + version))
  lazy val webuiUrl = opt[String]("webui_url",
    descr = "The http(s) url of the web ui, defaulting to the advertised hostname",
    noshort = true,
    default = None)
  lazy val reconciliationInterval = opt[Int]("reconciliation_interval",
    descr = "Reconciliation interval in seconds",
    default = Some(600))
  
  lazy val mesosAuthenticationPrincipal = opt[String]("mesos_authentication_principal",
    descr = "Mesos Authentication Principal",
    noshort = true
  )

  lazy val mesosAuthenticationSecretFile = opt[String]("mesos_authentication_secret_file",
    descr = "Mesos Authentication Secret",
    noshort = true
  )


  def zooKeeperHostAddresses: Seq[InetSocketAddress] =
    for (s <- zookeeperServers().split(",")) yield {
      val splits = s.split(":")
      require(splits.length == 2, "expected host:port for zk servers")
      new InetSocketAddress(splits(0), splits(1).toInt)
    }

  def zookeeperServers(): String = {
    if (zookeeperServersString().startsWith("zk://")) {
      return zookeeperServersString().replace("zk://", "").replaceAll("/.*", "")
    }
    zookeeperServersString()
  }

  def zooKeeperStatePath = "%s/state".format(zooKeeperPath())

  def zooKeeperCandidatePath = "%s/candidate".format(zooKeeperPath())
}
