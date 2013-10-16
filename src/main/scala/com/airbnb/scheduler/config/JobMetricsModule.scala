package com.airbnb.scheduler.config

import com.google.inject.{Provides, Scopes, AbstractModule, Singleton}
import com.airbnb.scheduler.jobs.{MetricReporterService, JobMetrics}
import com.codahale.metrics.MetricRegistry

/**
 * Author: @andykram
 */



class JobMetricsModule (config: GangliaConfiguration) extends AbstractModule {

  def configure() {
    bind(classOf[JobMetrics]).in(Scopes.SINGLETON)
  }

  @Provides
  @Singleton
  def provideMetricReporterService(registry: MetricRegistry) = {
    new MetricReporterService(config, registry)
  }
}
