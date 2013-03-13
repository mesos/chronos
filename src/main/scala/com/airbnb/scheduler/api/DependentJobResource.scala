package com.airbnb.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs.{Path, POST, Produces, PUT}
import javax.ws.rs.core.Response
import scala.Array

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.graph.JobGraph
import com.google.inject.Inject
import com.yammer.metrics.annotation.Timed

/**
 * The REST API for adding job-dependencies to the scheduler.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Create a case class that removes epsilon from the dependents.
@Path(PathConstants.dependentJobPath)
@Produces(Array("application/json"))
class DependentJobResource @Inject()(
    val jobScheduler: JobScheduler,
    val jobGraph: JobGraph,
    val configuration: SchedulerConfiguration) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @POST
  @Timed
  def post(job: DependencyBasedJob): Response = {
    try {
      log.info("Received request for job:" + job.toString)

      require(JobUtils.isValidJobName(job.name),
        "the job's name is invalid. Allowed names: '%s'".format(JobUtils.jobNamePattern.toString))
      if (job.parents.isEmpty) throw new Exception("Error, parent does not exist")

      jobScheduler.registerJob(List(job), true)
      Response.noContent().build()
    } catch {
      case ex: IllegalArgumentException => {
        log.log(Level.INFO, "Bad Request", ex)
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      }
      case ex: Throwable => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        return Response.serverError().build()
      }
    }
  }

  @PUT
  @Timed
  def put(job: DependencyBasedJob): Response = {
    try {
      val oldJobOpt = jobGraph.lookupVertex(job.name)
      require(!oldJobOpt.isEmpty, "Job '%s' not found".format(oldJobOpt.get.name))

      val oldJob = oldJobOpt.get.asInstanceOf[DependencyBasedJob]
      require(oldJob.getClass == job.getClass, "To update a job, the new job must be of the same type!")
      if (job.parents.isEmpty) throw new Exception("Error, parent does not exist")

      log.info("Received replace request for job:" + job.toString)
      require(!jobGraph.lookupVertex(job.name).isEmpty, "Job '%s' not found".format(job.name))
      //TODO(FL): Put all the logic for registering, deregistering and replacing dependency based jobs into one place.
      val parents = jobGraph.parentJobs(job)
      val oldParents = jobGraph.parentJobs(oldJob)
      oldParents.map(x => jobGraph.removeDependency(x.name, oldJob.name))
      log.info("Job parent: [ %s ], name: %s, command: %s".format(job.parents.mkString(","), job.name, job.command))
      jobGraph.replaceVertex(oldJob, job)
      parents.map(x => jobGraph.addDependency(x.name, job.name))
      log.info("Added job")
      Response.noContent().build()
    } catch {
      case ex: IllegalArgumentException => {
        log.log(Level.INFO, "Bad Request", ex)
        return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      }
      case ex: Throwable => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        return Response.serverError().build()
      }
    }
  }
}