package org.apache.mesos.chronos.scheduler.api

import org.apache.mesos.chronos.scheduler.jobs.constraints.{EqualsConstraint, LikeConstraint, UnlikeConstraint}
import org.apache.mesos.chronos.scheduler.jobs.{DependencyBasedJob, Container, EnvironmentVariable, ScheduleBasedJob, _}
import org.apache.mesos.chronos.utils.{JobDeserializer, JobSerializer}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.mesos.chronos.scheduler.jobs._
import org.joda.time.Minutes
import org.specs2.mutable.SpecificationWithJUnit

class SerDeTest extends SpecificationWithJUnit {

  "SerializerAndDeserializer" should {
    "serialize and deserialize a DependencyBasedJob correctly" in {
      JobDeserializer.config = null

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
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RO), None),
        Volume(None, "container/dir", None, None)
      )

      val networks = Seq(
        Network("testnet", Option(ProtocolType.IPv4), Seq(), Seq()),
        Network("testnet2", Option(ProtocolType.IPv4), Seq(Label("testlabel", "testvalue")), Seq())
      )

      val forcePullImage = false

      val parameters = scala.collection.mutable.ListBuffer[Parameter]()

      val container = Container("dockerImage", ContainerType.DOCKER, volumes, parameters, NetworkMode.BRIDGE, None, networks, forcePullImage)

      val arguments = Seq(
        "-testOne"
      )

      val constraints = Seq(
        EqualsConstraint("rack", "rack-1"),
        LikeConstraint("rack", "rack-[1-3]"),
        UnlikeConstraint("host", "foo")
      )

      val fetch = Seq(Fetch("https://mesos.github.io/chronos/", true, false, true))

      val a = new DependencyBasedJob(Set("B", "C", "D", "E"), "A", "noop", Minutes.minutes(5).toPeriod, 10L,
        20L, "fooexec", "fooflags", "", 7, "foo@bar.com", "Foo", "Test dependency-based job", "TODAY",
        "YESTERDAY", true, container = container, environmentVariables = environmentVariables,
        shell = false, arguments = arguments, softError = true, constraints = constraints, fetch = fetch)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[DependencyBasedJob])

      aCopy must_== a
    }

    "serialize and deserialize a ScheduleBasedJob correctly" in {
      JobDeserializer.config = null

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
        Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RW), None),
        Volume(None, "container/dir", None, None)
      )

      val networks = Seq(
        Network("testnet", Option(ProtocolType.IPv4), Seq(), Seq()),
        Network("testnet2", Option(ProtocolType.IPv4), Seq(Label("testlabel", "testvalue")), Seq())
      )

      val forcePullImage = true
      val parameters = scala.collection.mutable.ListBuffer[Parameter]()

      val container = Container("dockerImage", ContainerType.DOCKER, volumes, parameters, NetworkMode.HOST, None, networks, forcePullImage)

      val arguments = Seq(
        "-testOne"
      )

      val constraints = Seq(
        EqualsConstraint("rack", "rack-1"),
        LikeConstraint("rack", "rack-[1-3]"),
        UnlikeConstraint("host", "foo")
      )

      val fetch = Seq(Fetch("https://mesos.github.io/chronos/", true, false, true))

      val a = new ScheduleBasedJob("FOO/BAR/BAM", "A", "noop", Minutes.minutes(5).toPeriod, 10L, 20L,
        "fooexec", "fooflags", "", 7, "foo@bar.com", "Foo", "Test schedule-based job", "TODAY",
        "YESTERDAY", true, container = container, environmentVariables = environmentVariables,
        shell = true, arguments = arguments, softError = true, constraints = constraints, fetch = fetch)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[ScheduleBasedJob])

      aCopy must_== a
    }
  }

}
