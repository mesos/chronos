package com.airbnb.scheduler.api

import org.specs2.mutable.SpecificationWithJUnit

import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.jobs.DependencyBasedJob
import com.airbnb.scheduler.jobs.DockerContainer
import com.airbnb.scheduler.jobs.ScheduleBasedJob
import com.airbnb.scheduler.jobs.EnvironmentVariable
import com.airbnb.utils.JobDeserializer
import com.airbnb.utils.JobSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.joda.time.Minutes
import org.specs2.mutable.SpecificationWithJUnit
import com.airbnb.utils.{JobSerializer, JobDeserializer}

class SerDeTest extends SpecificationWithJUnit {

  "SerializerAndDeserializer" should {
    "serialize and deserialize a DependencyBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod =  new SimpleModule("JobModule")
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
      val container = DockerContainer("dockerImage", volumes)

      val a = new DependencyBasedJob(Set("B", "C", "D", "E"), "A", "noop", Minutes.minutes(5).toPeriod, 10L,
        20L, "fooexec", "fooflags", 7, "foo@bar.com", "TODAY", "YESTERDAY", true, container = container,
        environmentVariables = environmentVariables)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[DependencyBasedJob])

      aCopy must_== a
    }

    "serialize and deserialize a ScheduleBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod =  new SimpleModule("JobModule")
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
      val container = DockerContainer("dockerImage", volumes)

      val a = new ScheduleBasedJob("FOO/BAR/BAM", "A", "noop", Minutes.minutes(5).toPeriod, 10L, 20L,
        "fooexec", "fooflags", 7, "foo@bar.com", "TODAY", "YESTERDAY", true, container = container,
        environmentVariables = environmentVariables)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[ScheduleBasedJob])

      aCopy must_== a
    }
  }

}
