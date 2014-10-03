package com.airbnb.scheduler.api

import java.io.StringWriter
import java.util.logging.{Level, Logger}
import javax.ws.rs.{Consumes, GET, Path, Produces,  WebApplicationException}
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.core.Response.Status
import scala.Array

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.jobs.graph.Exporter
import com.airbnb.scheduler.graph.JobGraph
import com.google.inject.Inject
import com.codahale.metrics.annotation.Timed

/**
 * The REST API for managing jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.graphBasePath)
@Consumes(Array(MediaType.APPLICATION_JSON))
class GraphManagementResource @Inject()(
    val jobScheduler: JobScheduler,
    val jobGraph: JobGraph,
    val configuration: SchedulerConfiguration) {

  private val log = Logger.getLogger(getClass.getName)

  @Produces(Array(MediaType.TEXT_PLAIN))
  @Path(PathConstants.jobGraphDotPath)
  @GET
  @Timed
  def dotGraph(): Response = {
    try {
      return Response.ok(jobGraph.makeDotFile()).build
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }

  @Produces(Array(MediaType.TEXT_PLAIN))
  @Path(PathConstants.jobGraphCsvPath)
  @GET
  @Timed
  def jsonGraph(): Response = {
    try {
      val buffer = new StringWriter
      Exporter.export(buffer, jobGraph)
      return Response.ok(buffer.toString).build
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }
}