package com.airbnb.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs.{DELETE, Path, PathParam, Produces, PUT,  WebApplicationException}
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import scala.reflect.BeanProperty

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.state.PersistenceStore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import com.codahale.metrics.annotation.Timed
import org.apache.mesos.Protos.{TaskID, TaskStatus, TaskState}

/**
 * The REST API for managing tasks such as updating the status of an asynchronous task.
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

  /**
   * Updates the status of a job, especially useful for asynchronous jobs such as hadoop jobs.
   * @return
   */
  @Path("/{id}")
  @PUT
  @Timed
  def updateStatus(@PathParam("id") id: String, taskNotification: TaskNotification): Response = {
    log.info("Update request received")
    try {
      log.info("Received update for asynchronous taskId: %s, statusCode: %d".format(id, taskNotification.statusCode))

      if (taskNotification.statusCode == 0) {
        log.info("Task completed successfully '%s'".format(id))
        jobScheduler.handleFinishedTask(TaskStatus.newBuilder.setTaskId(TaskID.newBuilder.setValue(id)).setState(TaskState.TASK_FINISHED).build)
      } else {
        log.info("Task failed '%s'".format(id))
        jobScheduler.handleFailedTask(TaskStatus.newBuilder.setTaskId(TaskID.newBuilder.setValue(id)).setState(TaskState.TASK_FAILED).build)
      }
      Response.noContent().build()
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }

  @DELETE
  @Path(PathConstants.killTaskPattern)
  def killTasksForJob(@PathParam("jobName") jobName: String): Response = {
    log.info("Task purge request received")
    try {
      require(!jobGraph.lookupVertex(jobName).isEmpty, "Job '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      taskManager.cancelTasks(job)
      taskManager.removeTasks(job)
      return Response.noContent().build()
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }

  @DELETE
  @Path("/all")
  @Timed
  def purge(): Response = {
    log.info("Task purge request received")
    try {
      persistenceStore.purgeTasks()
      taskManager.queues.foreach(_.clear())
      return Response.noContent().build()
    } catch {
      case ex: Exception => {
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
      }
    }
  }
}

case class TaskNotification(@JsonProperty @BeanProperty statusCode: Int)
