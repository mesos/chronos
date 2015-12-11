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

import java.util.concurrent.Callable

import org.joda.time.DateTime

/**
 * A wrapper around a task-tuple (taskId, baseJob), which appends the tuple to the job queue once the task is due.
 * @author Florian Leibert (flo@leibert.de)
 */
class ScheduledTask(
  val taskId: String,
  val due: DateTime,
  val job: BaseJob,
  val taskManager: TaskManager)
    extends Callable[Void] {

  def call(): Void = {
    //TODO(FL): Think about pulling the state updating into the TaskManager.
    taskManager.log.info("Triggering: '%s'".format(job.name))
    taskManager.removeTaskFutureMapping(this)
    taskManager.enqueue(taskId, job.highPriority)
    null
  }
}
