package com.airbnb.scheduler.api

import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.{Context, MediaType, Response}
import scala.Array

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.graph.JobGraph
import com.google.inject.Inject
import com.codahale.metrics.annotation.Timed
import com.google.common.base.Charsets
import com.google.inject.name.Names
import com.fasterxml.jackson.databind.ObjectMapper
import javax.inject.Named
import com.airbnb.scheduler.jobs.ScheduleBasedJob

/**
 * The REST API to the iso8601 (timed, cron-like) component of the scheduler.
 * @author Florian Leibert (flo@leibert.de)
 */
@Path(PathConstants.iso8601JobPath)
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class Iso8601JobResource @Inject()(
  val jobScheduler: JobScheduler,
  val jobGraph: JobGraph,
  val configuration: SchedulerConfiguration) {

  private val log = Logger.getLogger(getClass.getName)

  val iso8601JobSubmissions = new AtomicLong(0)

  @POST
  @Timed
  def post(job: ScheduleBasedJob): Response = {
    try {
      log.info("Received request for job:" + job.toString)
      require(JobUtils.isValidJobName(job.name),
        "the job's name is invalid. Allowed names: '%s'".format(JobUtils.jobNamePattern.toString()))
      if (!Iso8601Expressions.canParse(job.schedule)) return Response.status(Response.Status.BAD_REQUEST).build()

      //TODO(FL): Create a wrapper class that handles adding & removing jobs!
      jobScheduler.registerJob(List(job), persist = true)
      iso8601JobSubmissions.incrementAndGet()
      log.info("Added job to JobGraph")
      Response.noContent().build()
    } catch {
      case ex: IllegalArgumentException => {
        log.log(Level.INFO, "Bad Request", ex)
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build
      }
      case ex: Throwable => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        return Response.serverError().build
      }
    }
  }

  @PUT
  @Timed
  def put(newJob: ScheduleBasedJob): Response = {
    try {
      val oldJobOpt = jobGraph.lookupVertex(newJob.name)
      require(!oldJobOpt.isEmpty, "Job '%s' not found".format(oldJobOpt.get.name))
      val oldJob = oldJobOpt.get
      require(oldJob.getClass == newJob.getClass, "To update a job, the new job must be of the same type!")

      if (!Iso8601Expressions.canParse(newJob.schedule)) {
        return Response.status(Response.Status.BAD_REQUEST).build()
      }

      jobScheduler.updateJob(oldJob, newJob)

      log.info("Replaced job: '%s', oldJob: '%s', newJob: '%s'".format(
        newJob.name,
        new String(JobUtils.toBytes(oldJob), Charsets.UTF_8),
        new String(JobUtils.toBytes(newJob), Charsets.UTF_8)))

      Response.noContent().build()
    } catch {
      case ex: IllegalArgumentException => {
        log.log(Level.INFO, "Bad Request", ex)
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build
      }
      case ex: Throwable => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        return Response.serverError().build
      }
    }
  }
}