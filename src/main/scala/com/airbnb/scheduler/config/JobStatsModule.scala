package com.airbnb.scheduler.config

import com.google.inject.{Provides, Scopes, AbstractModule, Singleton}
import com.airbnb.scheduler.jobs.JobStats
import com.datastax.driver.core.policies.{RoundRobinPolicy, LatencyAwarePolicy, DowngradingConsistencyRetryPolicy}
import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.Cluster

class JobStatsModule (config: CassandraConfiguration) extends AbstractModule {
  def configure() {
    bind(classOf[JobStats]).in(Scopes.SINGLETON)
  }

  @Provides
  @Singleton
  def provideCassandraClusterBuilder() = {
    config.cassandraContactPoints.get match {
      case Some(contactPoints) =>
        Some(
          Cluster.builder()
              .addContactPoints(contactPoints.split(","): _*)
              .withPort(config.cassandraPort())
              .withCompression(Compression.LZ4)
              .withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
              .withLoadBalancingPolicy(LatencyAwarePolicy.builder(new RoundRobinPolicy).build)
        )
      case _ =>
        None
    }
  }

  @Provides
  @Singleton
  def provideConfig() = {
    config
  }
}
