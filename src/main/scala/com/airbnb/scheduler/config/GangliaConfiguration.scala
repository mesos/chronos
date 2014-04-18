package com.airbnb.scheduler.config

import org.rogach.scallop.ScallopConf


trait GangliaConfiguration extends ScallopConf {

  lazy val gangliaHostPort = opt[String]("ganglia_host_port",
    descr = "Host and port for Ganglia",
    default = None)

  lazy val gangliaReportIntervalSeconds = opt[Long](
    "ganglia_reporting_interval",
    descr = "Ganglia reporting interval (seconds)",
    default = Some(60L))

  lazy val gangliaGroupPrefix = opt[String]("ganglia_group_prefix",
    descr = "Group prefix for Ganglia",
    default = Some(""))

  lazy val gangliaSpoofHost = opt[String]("ganglia_spoof",
    descr = "IP:host to spoof for Ganglia",
    default = None)
}
