package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs._
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.scheduler.state.PersistenceStore

import scala.beans.BeanProperty

/**
  * The REST API for managing tasks such as updating the status of an asynchronous task.
  *
  * @author Florian Leibert (flo@leibert.de)
  */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.taskBasePath)
@Produces(Array("application/json"))
class TaskManagementResource @Inject()(
                                        val persistenceStore: PersistenceStore,
                                        val jobScheduler: JobScheduler,
                                        val jobGraph: JobGraph,
                                        val taskManager: TaskManager,
                                        val configuration: SchedulerConfiguration) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @DELETE
  @Path(PathConstants.killTaskPattern)
  def killTasksForJob(@PathParam("jobName") jobName: String): Response = {
    log.info("Task purge request received")
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "JobSchedule '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      taskManager.cancelMesosTasks(job)
      Response.noContent().build()
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

case class TaskNotification(@JsonProperty @BeanProperty statusCode: Int)
