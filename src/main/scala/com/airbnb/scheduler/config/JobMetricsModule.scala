package com.airbnb.scheduler.config

import com.google.inject.{Scopes, AbstractModule}
import com.airbnb.scheduler.jobs.{MetricReporterService, JobMetrics}
import com.codahale.metrics.MetricRegistry

/**
 * Author: @andykram
 */



class JobMetricsModule (config: GangliaConfiguration) extends AbstractModule {

  private[this] val registry: MetricRegistry = new MetricRegistry()

  def configure() {
    bind(classOf[MetricRegistry]).toInstance(registry)
    bind(classOf[MetricReporterService]).toInstance(new MetricReporterService(config, registry))
    bind(classOf[JobMetrics]).in(Scopes.SINGLETON)
  }
}
