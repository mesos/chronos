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

import org.joda.time._
import org.specs2.mock._
import org.specs2.mutable._

class TaskUtilsSpec extends SpecificationWithJUnit with Mockito {

  "TaskUtils" should {
    "Get taskId" in {
      val schedule = "R/2012-01-01T00:00:01.000Z/P1M"
      val arguments = "-a 1 -b 2"
      val job1 = new ScheduleBasedJob(schedule, "sample-name", "sample-command", arguments = List(arguments))
      val job2 = new ScheduleBasedJob(schedule, "sample-name", "sample-command")
      val ts = 1420843781398L
      val due = new DateTime(ts)

      val taskIdOne = TaskUtils.getTaskId(job1, due, 0)
      val taskIdTwo = TaskUtils.getTaskId(job2, due, 0)

      taskIdOne must_== "ct:1420843781398:0:sample-name:" + arguments
      taskIdTwo must_== "ct:1420843781398:0:sample-name:"
    }

    "Get job arguments for taskId" in {
      val arguments = "-a 1 -b 2"
      var taskId = "ct:1420843781398:0:test:" + arguments
      val jobArguments = TaskUtils.getJobArgumentsForTaskId(taskId)

      jobArguments must_== arguments
    }

    "Parse taskId" in {
      val arguments = "-a 1 -b 2"
      val arguments2 = "-a 1:2 --B test"

      val taskIdOne = "ct:1420843781398:0:test:" + arguments
      val (jobName, jobDue, attempt, jobArguments) = TaskUtils.parseTaskId(taskIdOne)

      jobName must_== "test"
      jobDue must_== 1420843781398L
      attempt must_== 0
      jobArguments must_== arguments

      val taskIdTwo = "ct:1420843781398:0:test:" + arguments2
      val (_, _, _, jobArguments2) = TaskUtils.parseTaskId(taskIdTwo)

      jobArguments2 must_== arguments2

      val taskIdThree = "ct:1420843781398:0:test"
      val (jobName3, _, _, jobArguments3) = TaskUtils.parseTaskId(taskIdThree)

      jobName3 must_== "test"
      jobArguments3 must_== ""
    }
  }
}

