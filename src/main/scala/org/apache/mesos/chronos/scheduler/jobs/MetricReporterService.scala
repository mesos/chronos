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
package org.apache.mesos.chronos.scheduler.jobs

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.{ Graphite, GraphiteReporter }
import com.google.common.util.concurrent.AbstractIdleService
import org.apache.mesos.chronos.scheduler.config.GraphiteConfiguration

object MetricReporterService {

  object HostPort {
    def unapply(str: String): Option[(String, Int)] = str.split(":") match {
      case Array(host: String, port: String) => Some(Tuple2(host, port.toInt))
      case _                                 => None
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
      case _                         => // Nothing to shutdown!
    }
  }
}
