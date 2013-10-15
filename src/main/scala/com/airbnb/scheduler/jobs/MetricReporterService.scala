package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.codahale.metrics.ganglia.GangliaReporter
import java.util.concurrent.TimeUnit
import com.google.common.util.concurrent.AbstractIdleService

import com.codahale.metrics.MetricRegistry

object MetricReporterService {
  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _ => None
    }
  }
}

class MetricReporterService(config: SchedulerConfiguration,
                            registry: MetricRegistry)
    extends AbstractIdleService {
  private[this] var reporter: Option[GangliaReporter] = None

  def startUp() {
    this.reporter = config.gangliaHostPort match {
      case Some(MetricReporterService.HostPort(host: String, port: Int)) => {
        val reporter = new GangliaReporter(registry, host, port, config.gangliaGroupPrefix)
        reporter.start(config.gangliaReportIntervalSeconds, TimeUnit.SECONDS)
        Some(reporter)
      }
      case _ => None
    }
  }

  def shutDown() {
    this.reporter match {
      case Some(r: GangliaReporter) => r.shutdown()
      case _ => // Nothing to shutdown!
    }
  }
}
