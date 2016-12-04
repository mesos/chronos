package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * The REST API to the PerformanceResource component of the API.
  *
  * @author Matt Redmond (matt.redmond@airbnb.com)
  *         Returns a list of jobs, sorted by percentile run times.
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
      for ((jobNameString, time) <- jobs) {
        val myMap = Map("jobNameLabel" -> jobNameString, "time" -> time / 1000.0)
        output.append(myMap)
      }
      Response.ok(output).build
    } catch {
      case ex: IllegalArgumentException =>
        log.log(Level.INFO, "Bad Request", ex)
        Response
          .status(Status.BAD_REQUEST)
          .entity(new ApiResult(ExceptionUtils.getStackTrace(ex)))
          .build
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response
          .serverError()
          .entity(new ApiResult(ExceptionUtils.getStackTrace(ex),
            status = Status.INTERNAL_SERVER_ERROR.toString))
          .build
    }
  }
}
