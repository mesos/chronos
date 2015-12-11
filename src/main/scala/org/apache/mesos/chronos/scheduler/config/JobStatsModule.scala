/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ProtocolOptions.Compression
import com.datastax.driver.core.policies.{ DowngradingConsistencyRetryPolicy, LatencyAwarePolicy, RoundRobinPolicy }
import com.google.inject.{ AbstractModule, Provides, Scopes, Singleton }
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
            .withLoadBalancingPolicy(LatencyAwarePolicy.builder(new RoundRobinPolicy).build))
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
