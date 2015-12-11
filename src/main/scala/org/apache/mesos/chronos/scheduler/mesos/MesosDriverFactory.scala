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
package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Protos.{ FrameworkID, Status }
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.{ Scheduler, SchedulerDriver }

/**
 * The chronos driver doesn't allow calling the start() method after stop() has been called, thus we need a factory to
 * create a new driver once we call stop() - which will be called if the leader abdicates or is no longer a leader.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosDriverFactory(
    scheduler: Scheduler,
    frameworkIdUtil: FrameworkIdUtil,
    config: SchedulerConfiguration with HttpConf,
    schedulerDriverBuilder: SchedulerDriverBuilder = new SchedulerDriverBuilder) {

  private[this] val log = Logger.getLogger(getClass.getName)

  var mesosDriver: Option[SchedulerDriver] = None

  def start(): Unit = {
    val status = get().start()
    if (status != Status.DRIVER_RUNNING) {
      log.severe(s"MesosSchedulerDriver start resulted in status: $status. Committing suicide!")
      System.exit(1)
    }
  }

  def get(): SchedulerDriver = {
    if (mesosDriver.isEmpty) {
      mesosDriver = Some(makeDriver())
    }
    mesosDriver.get
  }

  private[this] def makeDriver(): SchedulerDriver = {
    import mesosphere.util.BackToTheFuture.Implicits.defaultTimeout

    import scala.concurrent.ExecutionContext.Implicits.global

    val maybeFrameworkID: Option[FrameworkID] = frameworkIdUtil.fetch
    schedulerDriverBuilder.newDriver(config, maybeFrameworkID, scheduler)
  }

  def close(): Unit = {
    assert(mesosDriver.nonEmpty, "Attempted to close a non initialized driver")
    if (mesosDriver.isEmpty) {
      log.severe("Attempted to close a non initialized driver")
      System.exit(1)
    }

    mesosDriver.get.stop(true)
    mesosDriver = None
  }
}
