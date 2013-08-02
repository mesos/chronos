package com.airbnb.scheduler.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.bazaarvoice.dropwizard.assets.{AssetsConfiguration, AssetsBundleConfiguration}
import com.yammer.dropwizard.config.Configuration
import org.hibernate.validator.constraints.NotEmpty

/**
 * Configuration values that may be parsed from a YAML file.
 * @author Florian Leibert (flo@leibert.de)
 */
class SchedulerConfiguration extends Configuration with AssetsBundleConfiguration {

  @NotEmpty
  @JsonProperty
  val master: String = "local"

  @JsonProperty
  val staticAssets: Boolean = true

  @NotEmpty
  @JsonProperty
  val user: String = "root"

  @JsonProperty
  val failoverTimeoutSeconds: Int = 1200

  @JsonProperty
  val scheduleHorizonSeconds: Int = 10

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
