package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs._

import com.codahale.metrics.annotation.Timed
import com.google.common.base.Charsets
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._

/**
 * The REST API for adding triggered jobs to the scheduler.
 */
@Path(PathConstants.triggeredJobPath)
@Produces(Array(MediaType.APPLICATION_JSON))
class TriggeredJobResource @Inject()(
                                      val jobScheduler: JobScheduler,
                                      val jobGraph: JobGraph) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @POST
  @Timed
  def post(newJob: TriggeredJob): Response = {
    handleRequest(newJob)
  }

  def handleRequest(newJob: TriggeredJob): Response = {
    try {
      val oldJobOpt = jobGraph.lookupVertex(newJob.name)
      if (oldJobOpt.isEmpty) {
        log.info("Received request for job:" + newJob.toString)

        require(JobUtils.isValidJobName(newJob.name),
          "the job's name is invalid. Allowed names: '%s'".format(JobUtils.jobNamePattern.toString()))

        jobScheduler.registerJob(List(newJob), persist = true)
        Response.noContent().build()
      } else {
        require(oldJobOpt.isDefined, "Job '%s' not found".format(newJob.name))

        val oldJob = oldJobOpt.get

        //TODO(FL): Ensure we're using job-ids rather than relying on jobs names for identification.
        assert(newJob.name == oldJob.name, "Renaming jobs is currently not supported!")

        log.info("Received replace request for job:" + newJob.toString)
        require(jobGraph.lookupVertex(newJob.name).isDefined, "Job '%s' not found".format(newJob.name))
        //TODO(FL): Put all the logic for registering, deregistering and replacing dependency based jobs into one place.

        jobScheduler.updateJob(oldJob, newJob)

        log.info("Replaced job: '%s', oldJob: '%s', newJob: '%s'".format(
          newJob.name,
          new String(JobUtils.toBytes(oldJob), Charsets.UTF_8),
          new String(JobUtils.toBytes(newJob), Charsets.UTF_8)))

        Response.noContent().build()
      }
    } catch {
      case ex: IllegalArgumentException =>
        log.log(Level.INFO, "Bad Request", ex)
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build()
    }
  }

  @PUT
  @Timed
  def put(newJob: TriggeredJob): Response = {
    handleRequest(newJob)
  }

}
