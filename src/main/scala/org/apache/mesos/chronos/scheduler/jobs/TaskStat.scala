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
package org.apache.mesos.chronos.scheduler.jobs

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{ DateTime, Duration }

/*
 * Task status enum at Chronos level, don't care about
 * killed/lost state. Idle state is not valid, a TaskStat
 * should only be generated if a task is already running
 * or finished
 */
object ChronosTaskStatus extends Enumeration {
  type TaskStatus = Value
  val Success, Fail, Running, Idle = Value
}

class TaskStat(@JsonProperty val taskId: String,
               @JsonProperty val jobName: String,
               @JsonProperty val taskSlaveId: String) {
  /*
   * Cassandra column names
   */
  val TASK_ID = "id"
  val JOB_NAME = "job_name"
  val TASK_EVENT_TIMESTAMP = "ts"
  val TASK_STATE = "task_state"
  val TASK_SLAVE_ID = "slave_id"

  //time stats
  @JsonProperty var taskStartTs: Option[DateTime] = None
  @JsonProperty var taskEndTs: Option[DateTime] = None
  @JsonProperty var taskDuration: Option[Duration] = None

  @JsonProperty var taskStatus: ChronosTaskStatus.Value = ChronosTaskStatus.Idle

  //move out of object later (this should be a data subclass)
  @JsonProperty var numElementsProcessed: Option[Long] = None
  //used only for output (HTTP GET)
  @JsonProperty var numAdditionalElementsProcessed: Option[Int] = None //used only for input (HTTP POST)

  def getTaskRuntime: Option[Duration] = taskDuration

  def setTaskStatus(status: ChronosTaskStatus.Value) = {
    //if already a terminal state, ignore
    if ((taskStatus != ChronosTaskStatus.Success) &&
      (taskStatus != ChronosTaskStatus.Fail)) {
      taskStatus = status
    }
  }

  def setTaskStartTs(startTs: Date) = {
    //update taskStartTs if new value is older
    val taskStartDatetime = new DateTime(startTs)
    taskStartTs = taskStartTs match {
      case Some(currTs: DateTime) =>
        if (taskStartDatetime.isBefore(currTs)) {
          Some(taskStartDatetime)
        }
        else {
          Some(currTs)
        }
      case None =>
        Some(taskStartDatetime)
    }

    taskEndTs match {
      case Some(ts) =>
        taskDuration = Some(new Duration(taskStartDatetime, ts))
      case None =>
    }
  }

  def setTaskEndTs(endTs: Date) = {
    val taskEndDatetime = new DateTime(endTs)
    taskEndTs = Some(taskEndDatetime)
    taskStartTs match {
      case Some(ts) =>
        taskDuration = Some(new Duration(ts, taskEndDatetime))
      case None =>
    }
  }

  override def toString: String = {
    "taskId=%s job_name=%s slaveId=%s startTs=%s endTs=%s duration=%s status=%s".format(
      taskId, jobName, taskSlaveId,
      taskStartTs.toString,
      taskEndTs.toString,
      taskDuration.toString,
      taskStatus.toString)
  }
}
