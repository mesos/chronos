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

import com.codahale.metrics.{ Counter, Histogram, MetricRegistry }
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.api.HistogramSerializer

import scala.collection.mutable

/**
 * Author: @andykram
 */
class JobMetrics @Inject() (registry: MetricRegistry) {

  protected val stats = new mutable.HashMap[String, Histogram]()
  protected val statuses = new mutable.HashMap[String, Map[String, Counter]]()
  protected val objectMapper = new ObjectMapper
  protected val mod = new SimpleModule("JobModule")

  mod.addSerializer(classOf[Histogram], new HistogramSerializer)
  objectMapper.registerModule(mod)

  def updateJobStat(jobName: String, timeMs: Long) {
    // Uses a Uniform Histogram by default for long term metrics.
    val stat: Histogram = stats.getOrElseUpdate(jobName, mkStat(jobName))

    stat.update(timeMs)
  }

  protected def mkStat(jobName: String, name: String = "time") = {
    registry.histogram(MetricRegistry.name("jobs", "run", name, jobName))
  }

  def getJsonStats(jobName: String): String = {
    val snapshot = getJobHistogramStats(jobName)
    objectMapper.writeValueAsString(snapshot)
  }

  def getJobHistogramStats(jobName: String): Histogram = {
    stats.getOrElseUpdate(jobName, mkStat(jobName))
  }

  def updateJobStatus(jobName: String, success: Boolean) {
    val statusCounters: Map[String, Counter] = statuses.getOrElseUpdate(jobName,
      Map("success" -> mkCounter(jobName, "success"),
        "failure" -> mkCounter(jobName, "failure")))

    val counter: Counter = if (success) {
      statusCounters.get("success").get
    }
    else {
      statusCounters.get("failure").get
    }

    counter.inc()
  }

  protected def mkCounter(jobName: String, name: String) = {
    registry.counter(MetricRegistry.name("jobs", "run", name, jobName))
  }

}
