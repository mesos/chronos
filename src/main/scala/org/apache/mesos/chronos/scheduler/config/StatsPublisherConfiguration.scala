package org.apache.mesos.chronos.scheduler.config

import org.rogach.scallop.ScallopConf

/**
  * Created by bthapaliya on 24/11/15.
  */
trait StatsPublisherConfiguration extends ScallopConf{
  lazy val webhookUrl = opt[String]("stats_webhook_url",
    descr = "Url to which tasks statistics are pushed",
    default = None)

  lazy val webhookPort = opt[Int]("stats_webhook_port",
    descr = "Port where the stats are pushed",
    default = Some(8080))

}
