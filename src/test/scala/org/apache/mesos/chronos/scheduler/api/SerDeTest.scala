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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.mesos.chronos.scheduler.jobs.constraints.{ EqualsConstraint, LikeConstraint }
import org.apache.mesos.chronos.scheduler.jobs.{ DependencyBasedJob, DockerContainer, EnvironmentVariable, ScheduleBasedJob, _ }
import org.apache.mesos.chronos.utils.{ JobDeserializer, JobSerializer }
import org.joda.time.Minutes
import org.specs2.mutable.SpecificationWithJUnit

class SerDeTest extends SpecificationWithJUnit {

  "SerializerAndDeserializer" should {
    "serialize and deserialize a DependencyBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod = new SimpleModule("JobModule")
      mod.addSerializer(classOf[BaseJob], new JobSerializer)
      mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
      objectMapper.registerModule(mod)

      val environmentVariables = Seq(
        EnvironmentVariable("FOO", "BAR"),
        EnvironmentVariable("AAAA", "BBBB"))

      val volumes = Seq(
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RO)),
        Volume(None, "container/dir", None))

      val forcePullImage = false

      var parameters = scala.collection.mutable.ListBuffer[Parameter]()

      val container = DockerContainer("dockerImage", volumes, parameters, NetworkMode.BRIDGE, forcePullImage)

      val arguments = Seq(
        "-testOne")

      val constraints = Seq(
        EqualsConstraint("rack", "rack-1"),
        LikeConstraint("rack", "rack-[1-3]"))

      val a = new DependencyBasedJob(Set("B", "C", "D", "E"), "A", "noop", Minutes.minutes(5).toPeriod, 10L,
        20L, "fooexec", "fooflags", 7, "foo@bar.com", "Foo", "Test dependency-based job", "TODAY",
        "YESTERDAY", true, container = container, environmentVariables = environmentVariables,
        shell = false, arguments = arguments, softError = true, constraints = constraints)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[DependencyBasedJob])

      aCopy must_== a
    }

    "serialize and deserialize a ScheduleBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod = new SimpleModule("JobModule")
      mod.addSerializer(classOf[BaseJob], new JobSerializer)
      mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
      objectMapper.registerModule(mod)

      val environmentVariables = Seq(
        EnvironmentVariable("FOO", "BAR"),
        EnvironmentVariable("AAAA", "BBBB"))

      val volumes = Seq(
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RW)),
        Volume(None, "container/dir", None))

      val forcePullImage = true
      var parameters = scala.collection.mutable.ListBuffer[Parameter]()

      val container = DockerContainer("dockerImage", volumes, parameters, NetworkMode.HOST, forcePullImage)

      val arguments = Seq(
        "-testOne")

      val constraints = Seq(
        EqualsConstraint("rack", "rack-1"),
        LikeConstraint("rack", "rack-[1-3]"))

      val a = new ScheduleBasedJob("FOO/BAR/BAM", "A", "noop", Minutes.minutes(5).toPeriod, 10L, 20L,
        "fooexec", "fooflags", 7, "foo@bar.com", "Foo", "Test schedule-based job", "TODAY",
        "YESTERDAY", true, container = container, environmentVariables = environmentVariables,
        shell = true, arguments = arguments, softError = true, constraints = constraints)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[ScheduleBasedJob])

      aCopy must_== a
    }
  }

}
