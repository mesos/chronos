package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import org.apache.mesos.Protos.TaskState
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import com.codahale.metrics.annotation.Timed
import com.google.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.HashMap
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
                                      val jobMetrics: JobMetrics) {

  private[this] val log = Logger.getLogger(getClass.getName)

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
          case j: ScheduleBasedJob =>
            children.foreach {
              child =>
                jobGraph.lookupVertex(child).get match {
                  case childJob: DependencyBasedJob =>
                    val newChild = new ScheduleBasedJob(
                      schedule = j.schedule,
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
      jobScheduler.sendNotification(job, "[Chronos] - Your job '%s' was deleted!".format(jobName))

      jobScheduler.deregisterJob(job, persist = true)
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

  @GET
  @Path(PathConstants.jobStatsPatternPath)
  def getStat(@PathParam("jobName") jobName: String): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "Job '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      Response.ok(jobMetrics.getJsonStats(jobName)).build()
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
      val jobs = ListBuffer[BaseJob]()
      import scala.collection.JavaConversions._
      jobGraph.dag.vertexSet().map({
        job =>
          jobs += jobGraph.getJobForName(job).get
      })

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

  @GET
  @Path(PathConstants.jobStatusPath)
  @Produces(Array(MediaType.APPLICATION_JSON))
  def status(@PathParam("jobName") jobName : String): Response = {
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "Job '%s' not found".format(jobName))

      /* Fetch job and task states for that job */
      val job:BaseJob = jobGraph.getJobForName(jobName).get
      val taskStatusList:List[TaskState] =
        jobScheduler.taskManager.getMesosTaskStates(job)

      /* Figure out status first by looking at task statuses and finally job fields */
      var status = "NOT RUN"
      if (taskStatusList != null & !taskStatusList.isEmpty ) {
        taskStatusList.head match {
          case TaskState.TASK_FAILED | TaskState.TASK_KILLED | TaskState.TASK_LOST => status = "FAILED"
          case TaskState.TASK_FINISHED => status = "SUCCESS"
          case TaskState.TASK_RUNNING | TaskState.TASK_STAGING | TaskState.TASK_STARTING => status = "RUNNING"
          case _ =>
            log.error("Could not figure out task state for value " + String.valueOf(_))
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
      } else {
        /* Just making sure that no null pointers can happen */
        val lastSuccess = if (job.lastSuccess != null) job.lastSuccess else ""
        val lastError = if (job.lastError != null) job.lastError else ""
        if (!"".equals(lastSuccess) & "".equals(lastError)) {
          // No failures have happened
          status = "SUCCESS"
        } else if ("".equals(lastSuccess) & !"".equals(lastError)) {
          // No success have happened
          status = "FAILURE"
        } else if (!lastSuccess.equals(lastError)) {
          // Compare timestamps
          status = if (lastSuccess.toLowerCase > lastError.toLowerCase) "SUCCESS" else "FAILED"
        }
      }

      /* Return json object with job name and status */
      Response.ok(Map(("job_name" -> jobName), ("status" -> status))).build()
    } catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Could not figure out task status", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }
}
