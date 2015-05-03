package org.apache.mesos.chronos.scheduler.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.policies.{DowngradingConsistencyRetryPolicy, LatencyAwarePolicy, RoundRobinPolicy}
import com.google.inject.{AbstractModule, Provides, Scopes, Singleton}
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats

class JobStatsModule(config: CassandraConfiguration) extends AbstractModule {
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
