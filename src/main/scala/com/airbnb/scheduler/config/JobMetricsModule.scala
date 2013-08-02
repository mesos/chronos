package com.airbnb.scheduler.config

import com.google.inject.{Scopes, AbstractModule}
import com.yammer.dropwizard.lifecycle.Managed
import com.yammer.metrics.core.MetricsRegistry
import com.yammer.metrics.reporting.GangliaReporter
import java.util.concurrent.TimeUnit
import com.airbnb.scheduler.jobs.JobMetrics

/**
 * Author: @andykram
 */

object JobMetricsModule {
  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _ => None
    }
  }
}
class JobMetricsModule (config: SchedulerConfiguration) extends AbstractModule with Managed {

  private[this] val registry: MetricsRegistry = new MetricsRegistry()
  private[this] var reporter: Option[GangliaReporter] = None

  def start() {
    this.reporter = config.gangliaHostPort match {
      case Some(JobMetricsModule.HostPort(host: String, port: Int)) => {
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

  def configure() {
    bind(classOf[MetricsRegistry]).toInstance(registry)
    bind(classOf[JobMetrics]).in(Scopes.SINGLETON)
  }
}
