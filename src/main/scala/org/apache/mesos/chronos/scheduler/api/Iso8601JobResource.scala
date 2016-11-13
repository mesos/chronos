package org.apache.mesos.chronos.scheduler.api

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.{ScheduleBasedJob, _}
import com.codahale.metrics.annotation.Timed
import com.google.common.base.Charsets
import com.google.inject.Inject

/**
 * The REST API to the iso8601 (timed, cron-like) component of the scheduler.
 * @author Florian Leibert (flo@leibert.de)
 */
@Path(PathConstants.iso8601JobPath)
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class Iso8601JobResource @Inject()(
                                    val jobScheduler: JobScheduler,
                                    val jobGraph: JobGraph) {

  val iso8601JobSubmissions = new AtomicLong(0)
  private val log = Logger.getLogger(getClass.getName)

  @POST
  @Timed
  def post(newJob: ScheduleBasedJob): Response = {
    try {
      val oldJobOpt = jobGraph.lookupVertex(newJob.name)
      if (oldJobOpt.isEmpty) {
        log.info("Received request for job:" + newJob.toString)
        require(JobUtils.isValidJobName(newJob.name),
          "the job's name is invalid. Allowed names: '%s'".format(JobUtils.jobNamePattern.toString()))
        if (!Iso8601Expressions.canParse(newJob.schedule, newJob.scheduleTimeZone))
          return Response.status(Response.Status.BAD_REQUEST).build()
        if (!JobUtils.isValidURIDefinition(newJob)) {
          log.warning(s"Tried to add both uri (deprecated) and fetch parameters on ${newJob.name}")
          return Response.status(Response.Status.BAD_REQUEST).build()
        }

        //TODO(FL): Create a wrapper class that handles adding & removing jobs!
        jobScheduler.loadJob(newJob)
        iso8601JobSubmissions.incrementAndGet()
        log.info("Added job to JobGraph")
        Response.noContent().build()
      } else {
        val oldJob = oldJobOpt.get

        if (!Iso8601Expressions.canParse(newJob.schedule, newJob.scheduleTimeZone)) {
          return Response.status(Response.Status.BAD_REQUEST).build()
        }

        oldJob match {
          case j: DependencyBasedJob =>
            val oldParents = jobGraph.parentJobs(j)
            oldParents.map(x => jobGraph.removeDependency(x.name, oldJob.name))
          case j: ScheduleBasedJob =>
        }

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
          .build
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build
    }
  }

}
