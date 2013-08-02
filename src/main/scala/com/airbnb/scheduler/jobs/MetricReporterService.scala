package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.yammer.dropwizard.lifecycle.Managed
import com.yammer.metrics.reporting.GangliaReporter
import java.util.concurrent.TimeUnit
import com.yammer.metrics.core.MetricsRegistry

object MetricReporterService {
  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _ => None
    }
  }
}

class MetricReporterService (config: SchedulerConfiguration, registry: MetricsRegistry) extends Managed {
  private[this] var reporter: Option[GangliaReporter] = None

  def start() {
    this.reporter = config.gangliaHostPort match {
      case Some(MetricReporterService.HostPort(host: String, port: Int)) => {
        val reporter = new GangliaReporter(registry, host, port, config.gangliaGroupPrefix)
        reporter.start(config.gangliaReportIntervalSeconds, TimeUnit.SECONDS)
        Some(reporter)
      }
      case _ => None
    }
  }

  def stop() {
    this.reporter match {
      case Some(r: GangliaReporter) => r.shutdown()
      case _ => // Nothing to shutdown!
    }
  }
}
