package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.Inject
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.mesos.chronos.scheduler.config.{CassandraConfiguration, SchedulerConfiguration}
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.scheduler.jobs.graph.Exporter
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

/**
  * The REST API for managing jobs.
  *
  * @author Florian Leibert (flo@leibert.de)
  */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.jobBasePath)
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class JobManagementResource @Inject()(val jobScheduler: JobScheduler,
                                      val jobGraph: JobGraph,
                                      val configuration: SchedulerConfiguration,
                                      val cassandraConfig: CassandraConfiguration,
                                      val jobStats: JobStats,
                                      val jobMetrics: JobMetrics) {

  private[this] val log = Logger.getLogger(getClass.getName)

  private val objectMapper = new ObjectMapper
  private val mod = new SimpleModule("JobManagementResourceModule")

  mod.addSerializer(classOf[JobStatWrapper], new JobStatWrapperSerializer)
  objectMapper.registerModule(mod)

  @Path(PathConstants.jobPatternPath)
  @DELETE
  @Timed
  def delete(@PathParam("jobName") jobName: String): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).nonEmpty, "JobSchedule '%s' not found".format(jobName))
      val job = jobGraph.lookupVertex(jobName).get
      val children = jobGraph.getChildren(jobName)
      if (children.nonEmpty) {
        job match {
          case j: DependencyBasedJob =>
            val parents = jobGraph.parentJobs(j)
            children.foreach {
              child =>
                val childJob = jobGraph.lookupVertex(child).get.asInstanceOf[DependencyBasedJob]
                val newParents = childJob.parents.filter { name => name != job.name } ++ j.parents
                val newChild = childJob.copy(parents = newParents)
                jobScheduler.replaceJob(childJob, newChild)
                parents.foreach { p =>
                  jobGraph.removeDependency(p.name, job.name)
                  jobGraph.addDependency(p.name, newChild.name)
                }
            }
          case j: ScheduleBasedJob =>
            children.foreach {
              child =>
                jobGraph.lookupVertex(child).get match {
                  case childJob: DependencyBasedJob =>
                    val newChild = ScheduleBasedJob(
                      schedule = j.schedule,
                      scheduleTimeZone = j.scheduleTimeZone,
                      name = childJob.name,
                      command = childJob.command,
                      successCount = childJob.successCount,
                      errorCount = childJob.errorCount,
                      executor = childJob.executor,
                      executorFlags = childJob.executorFlags,
                      taskInfoData = childJob.taskInfoData,
                      retries = childJob.retries,
                      owner = childJob.owner,
                      lastError = childJob.lastError,
                      lastSuccess = childJob.lastSuccess,
                      cpus = childJob.cpus,
                      disk = childJob.disk,
                      mem = childJob.mem,
                      disabled = childJob.disabled,
                      softError = childJob.softError,
                      uris = childJob.uris,
                      fetch = childJob.fetch,
                      highPriority = childJob.highPriority
                    )
                    jobScheduler.updateJob(childJob, newChild)
                  case _ =>
                }
            }
        }
      }
      // No need to send notifications here, the jobScheduler.deregisterJob will do it
      jobScheduler.deregisterJob(job)
      Response.noContent().build
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

  @GET
  @Path(PathConstants.jobStatsPatternPath)
  def getStat(@PathParam("jobName") jobName: String): Response = {
    try {
      val jobOpt = jobGraph.lookupVertex(jobName)
      require(jobOpt.nonEmpty, "Job '%s' not found".format(jobName))

      val histoStats = jobMetrics.getJobHistogramStats(jobName)
      val jobStatsList: List[TaskStat] = jobStats.getMostRecentTaskStatsByJob(jobOpt.get, cassandraConfig.jobHistoryLimit())
      val jobStatsWrapper = new JobStatWrapper(jobStatsList, histoStats)

      val wrapperStr = objectMapper.writeValueAsString(jobStatsWrapper)
      Response.ok(wrapperStr).build()
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

  @Path(PathConstants.jobPatternPath)
  @PUT
  @Timed
  def trigger(@PathParam("jobName") jobName: String,
              @QueryParam("arguments") arguments: String
             ): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "Job '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      log.info("Manually triggering job:" + jobName)
      jobScheduler.taskManager.enqueue(TaskUtils.getTaskId(job, DateTime.now(DateTimeZone.UTC), 0, Option(arguments).filter(_.trim.nonEmpty))
        , job.highPriority)
      Response.noContent().build
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

  @Path(PathConstants.jobPatternPath)
  @GET
  @Timed
  def trigger(@PathParam("jobName") jobName: String): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "Job '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      Response.ok(job).build()
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

  /**
    * Mark JobSchedule successful
    */
  @Path(PathConstants.jobSuccessPath)
  @PUT
  @Timed
  def markJobSuccessful(@PathParam("jobName") jobName: String): Response = {
    try {
      val success = jobScheduler.markJobSuccessAndFireOffDependencies(jobName)
      Response.ok("marked job %s as successful: %b".format(jobName, success)).build()
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


  /**
    * Allows an user to update the elements processed count for a job that
    * supports data tracking. The processed count has to be non-negative.
    */
  @POST
  @Path(PathConstants.jobTaskProgressPath)
  def updateTaskProgress(@PathParam("jobName") jobName: String,
                         @PathParam("taskId") taskId: String,
                         taskStat: TaskStat): Response = {
    try {
      val jobOpt = jobGraph.lookupVertex(jobName)
      require(jobOpt.nonEmpty, "JobSchedule '%s' not found".format(jobName))
      require(TaskUtils.isValidVersion(taskId), "Invalid task id format %s".format(taskId))
      require(jobOpt.get.dataProcessingJobType, "JobSchedule '%s' is not enabled to track data".format(jobName))

      taskStat.numAdditionalElementsProcessed.foreach {
        num =>
          //NOTE: 0 is a valid value
          require(num >= 0,
            "numAdditionalElementsProcessed (%d) is not positive".format(num))

          jobStats.updateTaskProgress(jobOpt.get, taskId, num)
      }
      Response.noContent().build
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

  @Path(PathConstants.allJobsPath)
  @GET
  @Timed
  def list(): Response = {
    try {
      import scala.collection.JavaConversions._
      val jobs = jobGraph.dag.vertexSet()
        .flatMap {
          jobGraph.getJobForName
        }
        .map {
          // copies fetch in uris or uris in fetch (only one can be set) __only__ in REST get, for compatibility
          case j: ScheduleBasedJob =>
            if (j.fetch.isEmpty) j.copy(fetch = j.uris.map {
              Fetch(_)
            })
            else j.copy(uris = j.fetch.map {
              _.uri
            })
          case j: DependencyBasedJob =>
            if (j.fetch.isEmpty) j.copy(fetch = j.uris.map {
              Fetch(_)
            })
            else j.copy(uris = j.fetch.map {
              _.uri
            })
        }
      Response.ok(jobs).build
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

  @Path(PathConstants.jobSummaryPath)
  @GET
  @Timed
  def getSummary(): Response = {
    try {
      val jobs = jobGraph.transformVertextSet(j => jobGraph.getJobForName(j))
        .map {
          job =>
            val state = Exporter.getLastState(job).toString
            val status = jobStats.getJobState(job.name).toString
            job match {
              case s: ScheduleBasedJob =>
                new JobSummary(job.name, state, status, s.schedule, List(), job.disabled)
              case d: DependencyBasedJob =>
                new JobSummary(job.name, state, status, "", d.parents.toList, job.disabled)
            }
        }
      Response.ok(new JobSummaryWrapper(jobs.toList)).build
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

  @GET
  @Path(PathConstants.jobSearchPath)
  @Timed
  def search(@QueryParam("name") name: String,
             @QueryParam("command") command: String,
             @QueryParam("any") any: String,
             @QueryParam("limit") limit: Integer,
             @QueryParam("offset") offset: Integer
            ) = {
    try {
      val jobs = jobGraph.transformVertextSet(j => jobGraph.getJobForName(j))

      val _limit: Integer = limit match {
        case x: Integer =>
          x
        case _ =>
          10
      }
      val _offset: Integer = offset match {
        case x: Integer =>
          x
        case _ =>
          0
      }

      val filteredJobs = jobs.filter {
        x =>
          var valid = true
          if (name != null && !name.isEmpty && !x.name.toLowerCase.contains(name.toLowerCase)) {
            valid = false
          }
          if (command != null && !command.isEmpty && !x.command.toLowerCase.contains(command.toLowerCase)) {
            valid = false
          }
          if (!valid && any != null && !any.isEmpty &&
            (x.name.toLowerCase.contains(any.toLowerCase) || x.command.toLowerCase.contains(any.toLowerCase))) {
            valid = true
          }
          // Maybe add some other query parameters?
          valid
      }.toList.slice(_offset, _offset + _limit)
      Response.ok(filteredJobs).build
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
