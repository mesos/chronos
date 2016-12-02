package org.apache.mesos.chronos.scheduler.api

import java.io.StringWriter
import java.util.logging.{Level, Logger}
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs._

import com.codahale.metrics.annotation.Timed
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.graph.Exporter
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats

/**
  * The REST API for managing jobs.
  *
  * @author Florian Leibert (flo@leibert.de)
  */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.graphBasePath)
@Consumes(Array(MediaType.APPLICATION_JSON))
class GraphManagementResource @Inject()(
                                         val jobStats: JobStats,
                                         val jobGraph: JobGraph,
                                         val configuration: SchedulerConfiguration) {

  private val log = Logger.getLogger(getClass.getName)

  @Produces(Array(MediaType.TEXT_PLAIN))
  @Path(PathConstants.jobGraphDotPath)
  @GET
  @Timed
  def dotGraph(): Response = {
    try {
      Response.ok(jobGraph.makeDotFile()).build
    } catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }

  @Produces(Array(MediaType.TEXT_PLAIN))
  @Path(PathConstants.jobGraphCsvPath)
  @GET
  @Timed
  def jsonGraph(): Response = {
    try {
      val buffer = new StringWriter
      Exporter.export(buffer, jobGraph, jobStats)
      Response.ok(buffer.toString).build
    } catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }
}
