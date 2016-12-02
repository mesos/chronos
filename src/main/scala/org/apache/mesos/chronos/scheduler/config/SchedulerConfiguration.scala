package org.apache.mesos.chronos.scheduler.config

import java.net.InetSocketAddress

import org.rogach.scallop.ScallopConf


/**
  * Configuration values that may be parsed from a YAML file.
  *
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

  lazy val clusterName = opt[String]("cluster_name",
    descr = "The name of the cluster where Chronos is run",
    default = None)
  lazy val zookeeperServersString = opt[String]("zk_hosts",
    descr = "The list of ZooKeeper servers for storing state",
    default = Some("localhost:2181"))
  lazy val hostname = opt[String]("hostname",
    descr = "The advertised hostname of this Chronos instance for network communication. This is used by other" +
      "Chronos instances and the Mesos master to communicate with this instance",
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
  lazy val zooKeeperAuth = opt[String]("zk_auth",
    descr = "Authorization string for ZooKeeper",
    default = None)
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
  lazy val mattermostWebhookUrl = opt[String]("mattermost_url",
    descr = "Webhook URL for posting to Mattermost",
    default = None)
  lazy val httpNotificationUrl = opt[String]("http_notification_url",
    descr = "Http URL for notifying failures",
    default = None)
  lazy val httpNotificationCredentials = opt[String]("http_notification_credentials",
    descr = "Http notification URL credentials in format username:password",
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
    default = Some("chronos"))
  lazy val webuiUrl = opt[String]("webui_url",
    descr = "The http(s) url of the web ui, defaulting to the advertised hostname",
    noshort = true,
    default = None)
  lazy val reconciliationInterval = opt[Int]("reconciliation_interval",
    descr = "Reconciliation interval in seconds",
    default = Some(600))
  lazy val mesosAuthenticationPrincipal = opt[String]("mesos_authentication_principal",
    descr = "Mesos Authentication Principal",
    noshort = true)
  lazy val mesosAuthenticationSecretFile = opt[String]("mesos_authentication_secret_file",
    descr = "Mesos Authentication Secret",
    noshort = true)
  lazy val reviveOffersForNewJobs = opt[Boolean]("revive_offers_for_new_jobs",
    descr = "Whether to call reviveOffers for new or changed jobs. (Default: do not use reviveOffers) ",
    default = Some(false))
  lazy val declineOfferDuration = opt[Long]("decline_offer_duration",
    descr = "(Default: Use mesos default of 5 seconds) " +
      "The duration (milliseconds) for which to decline offers by default",
    default = None)
  lazy val minReviveOffersInterval = opt[Long]("min_revive_offers_interval",
    descr = "Do not ask for all offers (also already seen ones) more often than this interval (ms). (Default: 5000)",
    default = Some(5000))


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
