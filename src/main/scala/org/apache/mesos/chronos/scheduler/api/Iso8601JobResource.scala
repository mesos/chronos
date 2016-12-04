package org.apache.mesos.chronos.scheduler.api

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import com.codahale.metrics.annotation.Timed
import com.google.common.base.Charsets
import com.google.inject.Inject
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.{ScheduleBasedJob, _}
import org.joda.time.{DateTime, DateTimeZone}

/**
  * The REST API to the iso8601 (timed, cron-like) component of the scheduler.
  *
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
        if (!Iso8601Expressions.canParse(newJob.schedule, newJob.scheduleTimeZone)) {
          val message = s"Cannot parse schedule '${newJob.schedule}' (wtf bro)"
          return Response
            .status(Status.BAD_REQUEST)
            .entity(new ApiResult(message))
            .build()
        }

        Iso8601Expressions.parse(newJob.schedule, newJob.scheduleTimeZone) match {
          case Some((_, startDate, _)) =>
            if (startDate.isBefore(new DateTime(DateTimeZone.UTC).minusYears(1))) {
              val message = s"Scheduled start date '${startDate.toString}' is more than 1 year in the past!"
              log.warning(message)
              return Response
                .status(Status.BAD_REQUEST)
                .entity(new ApiResult(message))
                .build()
            }
          case _ =>
            val message = s"Cannot parse schedule '${newJob.schedule}' (wtf bro)"
            log.warning(message)
            return Response
              .status(Status.BAD_REQUEST)
              .entity(new ApiResult(message))
              .build()
        }

        if (!JobUtils.isValidURIDefinition(newJob)) {
          val message = s"Tried to add both uri (deprecated) and fetch parameters on ${newJob.name}"
          log.warning(message)
          return Response
            .status(Status.BAD_REQUEST)
            .entity(new ApiResult(message))
            .build()
        }

        //TODO(FL): Create a wrapper class that handles adding & removing jobs!
        jobScheduler.loadJob(newJob)
        iso8601JobSubmissions.incrementAndGet()
        log.info("Added job to JobGraph")
        Response.noContent().build()
      } else {
        val oldJob = oldJobOpt.get

        if (!Iso8601Expressions.canParse(newJob.schedule, newJob.scheduleTimeZone)) {
          val message = s"Cannot parse schedule '${newJob.schedule}' (wtf bro)"
          log.warning(message)
          return Response
            .status(Status.BAD_REQUEST)
            .entity(new ApiResult(message))
            .build()
        }

        oldJob match {
          case j: DependencyBasedJob =>
            val oldParents = jobGraph.parentJobs(j)
            oldParents.foreach(x => jobGraph.removeDependency(x.name, oldJob.name))
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
