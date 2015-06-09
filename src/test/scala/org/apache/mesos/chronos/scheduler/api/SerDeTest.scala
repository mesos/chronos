package org.apache.mesos.chronos.scheduler.api

import org.apache.mesos.chronos.scheduler.jobs.constraints.EqualsConstraint
import org.apache.mesos.chronos.scheduler.jobs.{DependencyBasedJob, DockerContainer, EnvironmentVariable, ScheduleBasedJob, _}
import org.apache.mesos.chronos.utils.{JobDeserializer, JobSerializer}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
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
        EnvironmentVariable("AAAA", "BBBB")
      )

      val volumes = Seq(
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RO)),
        Volume(None, "container/dir", None)
      )

      val forcePullImage = false
      val container = DockerContainer("dockerImage", volumes, NetworkMode.BRIDGE, forcePullImage)

      val arguments = Seq(
        "-testOne"
      )

      val constraints = List(EqualsConstraint("rack", "rack-1"))

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
        EnvironmentVariable("AAAA", "BBBB")
      )

      val volumes = Seq(
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RW)),
        Volume(None, "container/dir", None)
      )

      val forcePullImage = true
      val container = DockerContainer("dockerImage", volumes, NetworkMode.HOST, forcePullImage)

      val arguments = Seq(
        "-testOne"
      )

      val constraints = List(EqualsConstraint("rack", "rack-1"))

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
