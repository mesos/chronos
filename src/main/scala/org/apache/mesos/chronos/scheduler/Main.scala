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
package org.apache.mesos.chronos.scheduler

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{ Level, Logger }

import mesosphere.chaos.http.{ HttpConf, HttpModule, HttpService }
import mesosphere.chaos.metrics.MetricsModule
import mesosphere.chaos.{ App, AppConfiguration }
import org.apache.mesos.chronos.scheduler.api._
import org.apache.mesos.chronos.scheduler.config._
import org.apache.mesos.chronos.scheduler.jobs.{ JobScheduler, MetricReporterService, ZookeeperService }
import org.rogach.scallop.ScallopConf

/**
 * Main entry point to chronos using the Chaos framework.
 * @author Florian Leibert (flo@leibert.de)
 */
object Main extends App {
  lazy val conf = new ScallopConf(args) with HttpConf with AppConfiguration with SchedulerConfiguration with GraphiteConfiguration with CassandraConfiguration
  val isLeader = new AtomicBoolean(false)
  private[this] val log = Logger.getLogger(getClass.getName)

  log.info("---------------------")
  log.info("Initializing chronos.")
  log.info("---------------------")

  def modules() = {
    Seq(
      new HttpModule(conf),
      new ChronosRestModule,
      new MetricsModule,
      new MainModule(conf),
      new ZookeeperModule(conf),
      new JobMetricsModule(conf),
      new JobStatsModule(conf))
  }

  try {
    run(
      classOf[ZookeeperService],
      classOf[HttpService],
      classOf[JobScheduler],
      classOf[MetricReporterService])
  }
  catch {
    case t: Throwable =>
      log.log(Level.SEVERE, s"Chronos has exited because of an unexpected error: ${t.getMessage}", t)
      System.exit(1)
  }
}
