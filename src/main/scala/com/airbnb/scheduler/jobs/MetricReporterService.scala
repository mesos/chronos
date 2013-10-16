package com.airbnb.scheduler.jobs

import com.airbnb.scheduler.config.{GangliaConfiguration, SchedulerConfiguration}
import com.codahale.metrics.ganglia.GangliaReporter
import java.util.concurrent.TimeUnit
import com.google.common.util.concurrent.AbstractIdleService

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.ganglia.GangliaReporter.Builder
import info.ganglia.gmetric4j.gmetric.GMetric
import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode

object MetricReporterService {
  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _ => None
    }
  }
}

class MetricReporterService(config: GangliaConfiguration,
                            registry: MetricRegistry)
    extends AbstractIdleService {
  private[this] var reporter: Option[GangliaReporter] = None

  def startUp() {
    this.reporter = config.gangliaHostPort() match {
      case MetricReporterService.HostPort(host: String, port: Int) => {
        val ganglia = new GMetric(host, port, UDPAddressingMode.MULTICAST, 1)
        val reporter = GangliaReporter.forRegistry(registry)
          .prefixedWith(config.gangliaGroupPrefix())
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build(ganglia)
        reporter.start(config.gangliaReportIntervalSeconds(), TimeUnit.SECONDS)
        Some(reporter)
      }
      case _ => None
    }
  }

  def shutDown() {
    this.reporter match {
      case Some(r: GangliaReporter) => r.stop()
      case _ => // Nothing to shutdown!
    }
  }
}
