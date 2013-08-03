package com.airbnb.scheduler.config

import com.google.inject.{Scopes, AbstractModule}
import com.yammer.dropwizard.lifecycle.Managed
import com.yammer.metrics.core.MetricsRegistry
import com.yammer.metrics.reporting.GangliaReporter
import java.util.concurrent.TimeUnit
import com.airbnb.scheduler.jobs.{MetricReporterService, JobMetrics}

/**
 * Author: @andykram
 */



class JobMetricsModule (config: SchedulerConfiguration) extends AbstractModule {

  private[this] val registry: MetricsRegistry = new MetricsRegistry()

  def configure() {
    bind(classOf[MetricsRegistry]).toInstance(registry)
    bind(classOf[MetricReporterService]).toInstance(new MetricReporterService(config, registry))
    bind(classOf[JobMetrics]).in(Scopes.SINGLETON)
  }
}
