/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{ Level, Logger }
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import javax.ws.rs.{ DELETE, PUT, Path, PathParam, Produces, WebApplicationException }

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import org.apache.mesos.Protos.{ TaskID, TaskState, TaskStatus }
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.scheduler.state.PersistenceStore

import scala.beans.BeanProperty

/**
 * The REST API for managing tasks such as updating the status of an asynchronous task.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.taskBasePath)
@Produces(Array("application/json"))
class TaskManagementResource @Inject() (
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
      }
      else {
        log.info("Task failed '%s'".format(id))
        jobScheduler.handleFailedTask(TaskStatus.newBuilder.setTaskId(TaskID.newBuilder.setValue(id)).setState(TaskState.TASK_FAILED).build)
      }
      Response.noContent().build()
    }
    catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }

  @DELETE
  @Path(PathConstants.killTaskPattern)
  def killTasksForJob(@PathParam("jobName") jobName: String): Response = {
    log.info("Task purge request received")
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "Job '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      taskManager.cancelTasks(job)
      taskManager.removeTasks(job)
      Response.noContent().build()
    }
    catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
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
      Response.noContent().build()
    }
    catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }
}

case class TaskNotification(@JsonProperty @BeanProperty statusCode: Int)
