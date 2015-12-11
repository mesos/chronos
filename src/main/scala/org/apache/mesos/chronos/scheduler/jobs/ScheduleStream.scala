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

/**
 * A stream of schedules.
 * Calling tail will return a clipped schedule.
 * The schedule consists of a string representation of an ISO8601 expression as well as a BaseJob.
 * @author Florian Leibert (flo@leibert.de)
 */
class ScheduleStream(val schedule: String, val jobName: String, val scheduleTimeZone: String = "") {

  def head: (String, String, String) = (schedule, jobName, scheduleTimeZone)

  /**
   * Returns a clipped schedule.
   * @return
   */
  def tail: Option[ScheduleStream] =
    //TODO(FL) Represent the schedule as a data structure instead of a string.
    Iso8601Expressions.parse(schedule, scheduleTimeZone) match {
      case Some((rec, start, per)) =>
        if (rec == -1)
          Some(new ScheduleStream(Iso8601Expressions.create(rec, start.plus(per), per), jobName,
            scheduleTimeZone))
        else if (rec > 0)
          Some(new ScheduleStream(Iso8601Expressions.create(rec - 1, start.plus(per), per), jobName,
            scheduleTimeZone))
        else
          None
      case None =>
        None
    }
}
