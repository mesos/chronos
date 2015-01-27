package org.apache.mesos.chronos.scheduler.config

import org.rogach.scallop.ScallopConf


trait GraphiteConfiguration extends ScallopConf {

  lazy val graphiteHostPort = opt[String]("graphite_host_port",
    descr = "Host and port (in the form `host:port`) for Graphite",
    default = None)

  lazy val graphiteReportIntervalSeconds = opt[Long](
    "graphite_reporting_interval",
    descr = "Graphite reporting interval (seconds)",
    default = Some(60L))

  lazy val graphiteGroupPrefix = opt[String]("graphite_group_prefix",
    descr = "Group prefix for Graphite",
    default = Some(""))
}
