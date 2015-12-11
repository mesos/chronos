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
import org.specs2.mutable._

class ScheduleStreamSpec extends SpecificationWithJUnit {

  val fakeCurrentTime = DateTime.parse("2012-01-01T00:00:00Z")

  "ScheduleStream" should {
    "return a properly clipped schedule" in {
      val orgSchedule = "R3/2012-01-01T00:00:00.000Z/P1D"
      val stream = new ScheduleStream(orgSchedule, null)
      stream.head must_== (orgSchedule, null, "")
      stream.tail.get.head must_== ("R2/2012-01-02T00:00:00.000Z/P1D", null, "")
      stream.tail.get.tail.get.head must_== ("R1/2012-01-03T00:00:00.000Z/P1D", null, "")
      stream.tail.get.tail.get.tail.get.head must_== ("R0/2012-01-04T00:00:00.000Z/P1D", null, "")
      stream.tail.get.tail.get.tail.get.tail must_== None
    }

    "return a infinite schedule when no repetition is specified" in {
      val orgSchedule = "R/2012-01-01T00:00:00.000Z/P1D"
      val stream = new ScheduleStream(orgSchedule, null)
      stream.head must_== (orgSchedule, null, "")
      stream.tail.get.head must_== ("R/2012-01-02T00:00:00.000Z/P1D", null, "")
    }
  }

}
