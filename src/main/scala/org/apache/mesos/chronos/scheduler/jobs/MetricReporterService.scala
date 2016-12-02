package org.apache.mesos.chronos.scheduler.jobs

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.google.common.util.concurrent.AbstractIdleService
import org.apache.mesos.chronos.scheduler.config.GraphiteConfiguration

object MetricReporterService {

  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _ => None
    }
  }

}

class MetricReporterService(config: GraphiteConfiguration,
                            registry: MetricRegistry)
  extends AbstractIdleService {
  private[this] var reporter: Option[GraphiteReporter] = None

  def startUp() {
    this.reporter = config.graphiteHostPort.get match {
      case Some(MetricReporterService.HostPort(host: String, port: Int)) =>
        val graphite = new Graphite(new InetSocketAddress(host, port))
        val reporter = GraphiteReporter.forRegistry(registry)
          .prefixedWith(config.graphiteGroupPrefix())
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build(graphite)
        reporter.start(config.graphiteReportIntervalSeconds(), TimeUnit.SECONDS)
        Some(reporter)
      case _ => None
    }
  }

  def shutDown() {
    this.reporter match {
      case Some(r: GraphiteReporter) => r.stop()
      case _ => // Nothing to shutdown!
    }
  }
}
