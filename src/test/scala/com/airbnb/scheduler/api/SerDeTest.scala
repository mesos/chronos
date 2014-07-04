package com.airbnb.scheduler.api

import com.airbnb.scheduler.jobs.{Container, ScheduleBasedJob, BaseJob, DependencyBasedJob}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.joda.time.Minutes
import org.specs2.mutable.SpecificationWithJUnit
import com.airbnb.utils.{ContainerDeserializer, ContainerSerializer, JobSerializer, JobDeserializer}

class SerDeTest extends SpecificationWithJUnit {

  "SerializerAndDeserializer" should {
    "serialize and deserialize a DependencyBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod =  new SimpleModule("JobModule")
      mod.addSerializer(classOf[BaseJob], new JobSerializer)
      mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
      mod.addSerializer(classOf[Container], new ContainerSerializer)

      objectMapper.registerModule(mod)

      val a = new DependencyBasedJob(Set("B", "C", "D", "E"), "A", "noop", Minutes.minutes(5).toPeriod, 10L,
        20L, "fooexec", "fooflags", Container("docker:///image", List("opt1", "opt2")), 7, "foo@bar.com", "TODAY", "YESTERDAY", true)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[DependencyBasedJob])

      aCopy must_== a
    }

    "serialize and deserialize a ScheduleBasedJob correctly" in {
      val objectMapper = new ObjectMapper
      val mod =  new SimpleModule("JobModule")
      mod.addSerializer(classOf[BaseJob], new JobSerializer)
      mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
      mod.addSerializer(classOf[Container], new ContainerSerializer)

      objectMapper.registerModule(mod)

      val a = new ScheduleBasedJob("FOO/BAR/BAM", "A", "noop", Minutes.minutes(5).toPeriod, 10L, 20L,
        "fooexec", "fooflags", Container("docker:///image", List("opt1", "opt2")), 7, "foo@bar.com", "TODAY", "YESTERDAY", true)

      val aStr = objectMapper.writeValueAsString(a)
      val aCopy = objectMapper.readValue(aStr, classOf[ScheduleBasedJob])

      aCopy must_== a
    }
  }

}
