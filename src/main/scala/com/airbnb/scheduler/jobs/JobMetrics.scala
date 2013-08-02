package com.airbnb.scheduler.jobs

import com.google.inject.Inject
import com.yammer.metrics.core.{MetricName, Counter, Histogram, MetricsRegistry}
import scala.collection.mutable
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.airbnb.scheduler.api.HistogramSerializer

/**
 * Author: @andykram
 */
class JobMetrics @Inject() (registry: MetricsRegistry) {

  protected val stats = new mutable.HashMap[String, Histogram]()
  protected val statuses = new mutable.HashMap[String, Map[String, Counter]]()
  protected val objectMapper = new ObjectMapper
  protected val mod =  new SimpleModule("JobModule")

  mod.addSerializer(classOf[Histogram], new HistogramSerializer)
  objectMapper.registerModule(mod)

  protected def mkStat(jobName: String, name: String = "time") = {
    registry.newHistogram(new MetricName("jobs", "run", name, jobName), false)
  }
  protected def mkCounter(jobName: String, name: String) = {
    registry.newCounter(new MetricName("jobs", "run", name, jobName))
  }

  def updateJobStat(jobName: String, timeMs: Long) {
    // Uses a Uniform Histogram by default for long term metrics.
    val stat: Histogram = stats.getOrElseUpdate(jobName, mkStat(jobName))

    stat.update(timeMs)
  }

  def getJsonStats(jobName: String): String = {
    val snapshot = stats.getOrElseUpdate(jobName, mkStat(jobName))
    objectMapper.writeValueAsString(snapshot)
  }

  def updateJobStatus(jobName: String, success: Boolean) {
    val statusCounters: Map[String, Counter] = statuses.getOrElseUpdate(jobName,
      Map("success" -> mkCounter(jobName, "success"),
          "failure" -> mkCounter(jobName, "failure")))

    val counter: Counter = if (success) {
      statusCounters.get("success").get
    } else {
      statusCounters.get("failure").get
    }

    counter.inc()
  }

}
