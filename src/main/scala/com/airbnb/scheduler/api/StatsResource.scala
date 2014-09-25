package com.airbnb.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.core.Response.Status
import scala.Array

import com.airbnb.scheduler.config.SchedulerConfiguration
import scala.collection.mutable.ListBuffer
import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.graph.JobGraph
import com.google.inject.Inject
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.JavaConversions._
/**
 * The REST API to the PerformanceResource component of the API.
 * @author Matt Redmond (matt.redmond@airbnb.com)
 * Returns a list of jobs, sorted by percentile run times.
 */

@Path(PathConstants.allStatsPath)
@Produces(Array(MediaType.APPLICATION_JSON))
class StatsResource @Inject()(
                                     val jobScheduler: JobScheduler,
                                     val jobGraph: JobGraph,
                                     val configuration: SchedulerConfiguration,
                                     val jobMetrics: JobMetrics) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @Timed
  @GET
  // Valid arguments are
  // /scheduler/stats/99thPercentile
  // /scheduler/stats/98thPercentile
  // /scheduler/stats/95thPercentile
  // /scheduler/stats/75thPercentile
  // /scheduler/stats/median
  // /scheduler/stats/mean
  def getPerf(@PathParam("percentile") percentile: String): Response = {
    try {
      var output = ListBuffer[Map[String, Any]]()
      var jobs = ListBuffer[(String, Double)]()

      val mapper = new ObjectMapper()
      for (jobNameString <- jobGraph.dag.vertexSet()) {
        val node = mapper.readTree(jobMetrics.getJsonStats(jobNameString))
        if (node.has(percentile) && node.get(percentile) != null) {
          val time = node.get(percentile).asDouble()
          jobs.append((jobNameString, time))
        }
      }
      jobs = jobs.sortBy(_._2).reverse
      for ( (jobNameString, time) <- jobs) {
        val myMap = Map("jobNameLabel" -> jobNameString, "time" -> time / 1000.0)
        output.append(myMap)
      }
      Response.ok(output).build
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }
}
