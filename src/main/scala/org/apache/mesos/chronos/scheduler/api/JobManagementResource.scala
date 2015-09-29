package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import org.apache.mesos.chronos.scheduler.config.{CassandraConfiguration, SchedulerConfiguration}
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

import com.codahale.metrics.annotation.Timed
import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

/**
 * The REST API for managing jobs.
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
  private val mod =  new SimpleModule("JobManagementResourceModule")

  mod.addSerializer(classOf[JobStatWrapper], new JobStatWrapperSerializer)
  objectMapper.registerModule(mod)

  @Path(PathConstants.jobPatternPath)
  @DELETE
  @Timed
  def delete(@PathParam("jobName") jobName: String): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).nonEmpty, "Job '%s' not found".format(jobName))
      val job = jobGraph.lookupVertex(jobName).get
      val children = jobGraph.getChildren(jobName)
      if (children.nonEmpty) {
        job match {
          case j: DependencyBasedJob =>
            val parents = jobGraph.parentJobs(j)
            children.foreach {
              child =>
                val childJob = jobGraph.lookupVertex(child).get.asInstanceOf[DependencyBasedJob]
                val newParents = childJob.parents.filter { name => name != job.name} ++ j.parents
                val newChild = childJob.copy(parents = newParents)
                jobScheduler.replaceJob(childJob, newChild)
                parents.foreach { p =>
                  jobGraph.removeDependency(p.name, job.name)
                  jobGraph.addDependency(p.name, newChild.name)
                }
            }
          case j: InternalScheduleBasedJob =>
            children.foreach {
              child =>
                jobGraph.lookupVertex(child).get match {
                  case childJob: DependencyBasedJob =>
                    val newChild = new InternalScheduleBasedJob(
                      scheduleData = j.scheduleData,
                      scheduleTimeZone = j.scheduleTimeZone,
                      name = childJob.name,
                      command = childJob.command,
                      epsilon = childJob.epsilon,
                      successCount = childJob.successCount,
                      errorCount = childJob.errorCount,
                      executor = childJob.executor,
                      executorFlags = childJob.executorFlags,
                      retries = childJob.retries,
                      owner = childJob.owner,
                      lastError = childJob.lastError,
                      lastSuccess = childJob.lastSuccess,
                      async = childJob.async,
                      cpus = childJob.cpus,
                      disk = childJob.disk,
                      mem = childJob.mem,
                      disabled = childJob.disabled,
                      softError = childJob.softError,
                      uris = childJob.uris,
                      highPriority = childJob.highPriority
                    )
                    jobScheduler.updateJob(childJob, newChild)
                  case _ =>
                }
            }
        }
      }
      // No need to send notifications here, the jobScheduler.deregisterJob will do it
      jobScheduler.deregisterJob(job, persist = true)
      Response.noContent().build
    } catch {
      case ex: IllegalArgumentException => {
        log.log(Level.INFO, "Bad Request", ex)
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      }
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build
      }
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
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build
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
      jobScheduler.taskManager.enqueue(TaskUtils.getTaskId(job, DateTime.now(DateTimeZone.UTC), 0), job.highPriority)
      Response.noContent().build
    } catch {
      case ex: IllegalArgumentException =>
        log.log(Level.INFO, "Bad Request", ex)
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build
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
          taskStat: TaskStat) : Response = {
    try {
      val jobOpt = jobGraph.lookupVertex(jobName)
      require(jobOpt.nonEmpty, "Job '%s' not found".format(jobName))
      require(TaskUtils.isValidVersion(taskId), "Invalid task id format %s".format(taskId))
      require(jobOpt.get.dataProcessingJobType, "Job '%s' is not enabled to track data".format(jobName))

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
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage).build
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build
    }
  }

  @Path(PathConstants.allJobsPath)
  @GET
  @Timed
  def list(): Response = {
    try {
      val jobs = ListBuffer[BaseJob]()
      import scala.collection.JavaConversions._
      jobGraph.dag.vertexSet().map({
        job =>
          jobs += jobGraph.getJobForName(job).get
      })
      Response.ok(jobs.toList).build
    } catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
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
      import scala.collection.JavaConversions._

      val jobs = jobGraph.dag.vertexSet().flatMap(jobGraph.getJobForName)

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
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }

}
