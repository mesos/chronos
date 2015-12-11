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
package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Scheduler
import org.apache.mesos.chronos.ChronosTestHelper._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class MesosDriverFactorySpec extends SpecificationWithJUnit with Mockito {
  "MesosDriverFactorySpec" should {
    "always fetch the frameworkId from the state store before creating a driver" in {
      val scheduler: Scheduler = mock[Scheduler]
      val frameworkIdUtil: FrameworkIdUtil = mock[FrameworkIdUtil]
      val mesosDriverFactory = new MesosDriverFactory(
        scheduler,
        frameworkIdUtil,
        makeConfig(),
        mock[SchedulerDriverBuilder])

      frameworkIdUtil.fetch(any, any).returns(None)

      mesosDriverFactory.get()

      there was one(frameworkIdUtil).fetch(any, any)
      ok
    }
  }
}
